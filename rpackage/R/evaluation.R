require(splines)

rawDataLoad <- function(gse6865_tsv_file) {
  gse6865_raw_df = read.delim(gse6865_tsv_file)

  gse6865_raw = as.matrix(gse6865_raw_df[,2:15])

  gse6865_raw_time = c(0,5,10,15,20,25,30,40,50,60,70,80,90,100)
  rownames(gse6865_raw) = gse6865_raw_df$ID_REF

  gse6865_raw[is.na(gse6865_raw[,1]),1] = 0
  na2 = is.na(gse6865_raw[,2])
  gse6865_raw[na2,2] = 0.5 * gse6865_raw[na2,1] + 0.5 * gse6865_raw[na2,3]

  gse6865_splined = splineProfileMatrix(gse6865_raw, gse6865_raw_time, 0:100, df = 6)
}

splineProfileMatrix <- function(profileMatrix, time, targetTime, df, degree = 3) {
  if(any(is.na(profileMatrix))) {
    stop("Profiles cannot be NA")
  }
  splineBasis = bs(time, df = df,degree = degree)
  #Use a least-squares fit of a B-spline basis
  splineFit <- lm(t(profileMatrix) ~ 0 + splineBasis); #Without intercept
  basisNew = bs(x = targetTime, degree = degree, knots = attr(splineBasis, "knots"), Boundary.knots = attr(splineBasis, "Boundary.knots"));

  splinedResult = t(basisNew %*% splineFit$coefficients);
  splinedResult[splinedResult < 0] = 0;
  rownames(splinedResult) = rownames(profileMatrix)
  return(splinedResult)
}


computeRegulon <- function(deviceSpecs, profiles, regulatorName, regulonNames, errorDef = list(absolute = 0, relative = 0.2, minimal = 0.5), minFitQuality = 0.8) {
  if(is.null(rownames(profiles))) {
    stop("Profiles must have associated rownames");
  }
  targetProfiles = rownames(profiles) %in% regulonNames


  #Test flat
  constantProfiles = testConstant(profiles, errorDef)

  #Test constant synthesis
  profilesToTestConstantSynthesis = targetProfiles & !constantProfiles;
  profilesToTestConstantSynthesisIndices = which(profilesToTestConstantSynthesis);

  if(length(profilesToTestConstantSynthesisIndices) <= 0) {
    constantSynthesisResults = list();
    constantSynthesisProfiles = logical(0);

    regulationResults = list();
    actualTargets = logical(0);
    numTargets = 0;
    regulated = logical(0);
  } else {

    constantSynthesisResults = computeConstantSynthesis(deviceSpecs, profiles, tasks = profilesToTestConstantSynthesisIndices);

    constantSynthesisProfiles = testConstantSynthesis(constantSynthesisResults, errorDef, minFitQuality);

    #Run the actual prediction
    regulatorIndex = which(rownames(profiles) == regulatorName)

    actualTargets = targetProfiles & !constantProfiles & !constantSynthesisProfiles;
    numTargets = sum(actualTargets);

    if(numTargets <= 0) {
      regulationResults = list();
      actualTargets = logical(0);
      numTargets = 0;
      regulated = logical(0);
    }
    else {
      tasks = array(0, c(numTargets,2));
      tasks[,1] = regulatorIndex;
      tasks[,2] = which(actualTargets);

      #constraints = array(1, )

      regulationResults = computeAdditiveRegulation(deviceSpecs, profiles, tasks, constraints = "+")
      regulated = testAdditiveRegulation(regulationResults, errorDef, minFitQuality)
    }
  }

  return( list(
    constantSynthesisResults = constantSynthesisResults,
    regulationResults = regulationResults,
    numConstant = sum(targetProfiles & constantProfiles),
    constant = targetProfiles & constantProfiles,
    numConstantSynthesis = sum(constantSynthesisProfiles),
    constantSynthesis = constantSynthesisProfiles,
    numTested = numTargets,
    tested = actualTargets,
    numRegulated = sum(regulated),
    regulated = regulated
  ))
}

generateRandomProfile <- function(time, scale, length) {
  covMatrix = array(0,c(length(time), length(time)));
  for(i in 1:length(time))  {
    covMatrix[i,i] = scale^2 + 0.00001;
    if (i < length(time)) {
      for(j in (i+1):length(time)) {
        covariance = (scale^2) * exp( (-0.5 / (length ^ 2)) * ((time[i] - time[j]) ^ 2) );
        covMatrix[i,j] = covariance
        covMatrix[j,i] = covariance
      }
    }
  }
  cholCov = t(chol(covMatrix));
  rawProfile = cholCov %*% rnorm(length(time));
  #return(rawProfile)
  return(t(log(1 + exp(rawProfile))))
}

generateUsefulRandomProfile <- function(time, scale, length, errorDef, originalProfile) {
  repeat { #Rejection sampling to get a non-flat, non-similar profile
    randomProfile = generateRandomProfile(time, scale, length);
    if(!testConstant(randomProfile, errorDef)) {
      if(is.null(originalProfile) || cor(originalProfile, randomProfile) < 0.95) {
        break;
      }
    }
  }
  return(randomProfile)
}


plotRandomProfiles <- function(n, time, scale, length, trueProfile = NULL) {
  profiles = array(0, c(n, length(time)));
  for(i in 1:n) {
    profiles[i,] = generateRandomProfile(time, scale, length);
  }
  matplot(time, t(profiles), type = "l")
  if(!is.null(trueProfile)) {
    points(time, trueProfile)
  }
}

testRandomRegulator <- function(deviceSpecs, rounds, profiles, time, scale, length, originalProfile, errorDef = defaultErrorDef()) {
  profilesDim = dim(profiles);
  numProfiles = profilesDim[1];
  numTime = profilesDim[2];
  profilesWithRegulator = array(0, c(numProfiles + 1, numTime));
  profilesWithRegulator[1:numProfiles,] = profiles;

  fitQualities = array(0, c(rounds, numProfiles));
  randomProfiles = array(0, c(rounds, length(time)));

  tasks = array(0, c(numProfiles,2));
  tasks[,1] = numProfiles + 1 #The regulator
  tasks[,2] = 1:numProfiles; #The targets

  constraints = "+";

  for(round in 1:rounds) {
    randomProfile = generateUsefulRandomProfile(time, scale, length, errorDef, originalProfile)
    randomProfiles[round, ] = randomProfile;

    profilesWithRegulator[profilesDim[1] + 1,] = randomProfile;
    computationResult = computeAdditiveRegulation(deviceSpecs, profilesWithRegulator, tasks, constraints = constraints);
    fittedProfiles = evaluateAdditiveRegulationResult(computationResult, time);
    numFits = 0;
    for(i in 1:numProfiles) {
      fitQualities[round,i] = fitQuality(profiles[i,], fittedProfiles[i,], errorDef)
    }

  }
  return(list(fitQualities = fitQualities,
              randomProfiles = randomProfiles));
}

evaluateRandomForRegulon <- function(deviceSpecs, rounds, profiles, regulatorName, regulonNames, time, scale, length, errorDef = defaultErrorDef(), minFitQuality = 0.8) {
  trueResults = computeRegulon(deviceSpecs, profiles, regulatorName, regulonNames, errorDef = errorDef, minFitQuality = minFitQuality)

  originalProfile = profiles[rownames(profiles) == regulatorName,]
  randomResults = testRandomRegulator(deviceSpecs, rounds, profiles[trueResults$tested,], time, scale, length, originalProfile, errorDef);

  trueRatio = trueResults$numRegulated / trueResults$numTested;
  randomRatios = rowMeans(randomResults$fitQualities > minFitQuality);

  return(list(
    trueResults = trueResults,
    trueRatio = trueRatio,
    randomResults = randomResults,
    randomRatios = randomRatios,
    overallRandomRatio = mean(randomResults$fitQualities > minFitQuality),
    regulonQuantile = ecdf(randomRatios)(trueRatio)
  ))
}

testVariousSplines <- function(deviceSpecs, rawProfiles, rawTime, targetTime, dfsToTest, regulatorName, regulonNames, scale, length, errorDef = defaultErrorDef(), minFitQuality = 0.8) {
  results = list();
  for(i in 1:length(dfsToTest)) {
    profiles = splineProfileMatrix(rawProfiles, rawTime, targetTime, dfsToTest[i]);
    currentResult = evaluateRandomForRegulon(deviceSpecs, 100, profiles, regulatorName, regulonNames, targetTime, scale, length, errorDef, minFitQuality)
    results[[i]] = list(df = dfsToTest[i],
                      result = currentResult)
  }
  for(i in 1:length(dfsToTest)) {
    cat(paste0("DF: ", dfsToTest[i], "\nTrue ratio: ", results[[i]]$result$trueRatio, "\nOverall random: ", results[[i]]$result$overallRandomRatio, "\n\n"));
  }
  return(results);
}
