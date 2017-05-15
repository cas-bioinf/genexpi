computeSigB <- function(profiles, sigBRegulonNames, errorDef = list(absolute = 0, relative = 0.2, minimal = 0.5), minFitQuality = 0.8) {
  targetProfiles = rownames(profiles) %in% sigBRegulonNames


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

    constantSynthesisResults = computeConstantSynthesis(getDeviceSpecs(), profiles, tasks = profilesToTestConstantSynthesisIndices);

    constantSynthesisProfiles = testConstantSynthesis(constantSynthesisResults, errorDef, minFitQuality);

    #Run the actual prediction
    sigBIndex = which(rownames(profiles) == "sigB")

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
      tasks[,1] = sigBIndex;
      tasks[,2] = which(actualTargets);

      #constraints = array(1, )

      regulationResults = computeAdditiveRegulation(getDeviceSpecs(), profiles, tasks)
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

plotRandomProfiles <- function(n, time, scale, length) {
  profiles = array(0, c(n, length(time)));
  for(i in 1:n) {
    profiles[i,] = generateRandomProfile(time, scale, length);
  }
  matplot(time, t(profiles), type = "l")
}

testRandomRegulator <- function(deviceSpecs, rounds, profiles, time, scale, length, errorDef = defaultErrorDef()) {
  profilesDim = dim(profiles);
  numProfiles = profilesDim[1];
  numTime = profilesDim[2];
  profilesWithRegulator = array(0, c(numProfiles + 1, numTime));
  profilesWithRegulator[1:numProfiles,] = profiles;

  fitQualities = array(0, c(rounds, numProfiles));

  tasks = array(0, c(numProfiles,2));
  tasks[,1] = numProfiles + 1 #The regulator
  tasks[,2] = 1:numProfiles; #The targets

  constraints = NULL #TODO positive only

  for(round in 1:rounds) {
    randomProfile = generateRandomProfile(time, scale, length);
    if(testConstant(randomProfile, errorDef)) {
      fitQualities[round,] = NA;
    }
    else {
      profilesWithRegulator[profilesDim[1] + 1,] = randomProfile;
      computationResult = computeAdditiveRegulation(deviceSpecs, profilesWithRegulator, tasks, constraints = constraints);
      fittedProfiles = evaluateAdditiveRegulationResult(computationResult, time);
      numFits = 0;
      for(i in 1:numProfiles) {
        fitQualities[round,i] = fitQuality(profiles[i,], fittedProfiles[i,], errorDef)
      }

    }
  }
  return(fitQualities);
}
