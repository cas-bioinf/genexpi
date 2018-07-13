#Measures performance of a set of random profiles in explaining the profiles in regulon
testRandomRegulator <- function(deviceSpecs, rounds, profiles, time, randomScale, randomLength, rawTime, originalRawProfile, splineDFs, errorDef = defaultErrorDef(), splineIntercept = FALSE, constraints = "+") {
  profilesDim = dim(profiles);
  numProfiles = profilesDim[1];
  numTime = profilesDim[2];

  timeStep = getTimeStep(time)

  fitQualities = array(0, c(rounds, numProfiles));

  parameters = list()

  randomProfilesRaw = array(0, c(rounds, length(rawTime)));
  for(round in 1:rounds) {
    randomProfilesRaw[round,] = generateUsefulRandomProfile(rawTime, randomScale, randomLength, errorDef, originalRawProfile)
  }
  randomProfiles = splineProfileMatrix(randomProfilesRaw, rawTime, time, splineDFs, intercept = splineIntercept);

  #Cache the java object and reuse it
  profilesWithRandomJava = geneProfilesFromMatrix(rbind(profiles, randomProfiles))

  #The first element (regulator) of tasks changes, the second stays the same
  tasks = array(0, c(numProfiles,2));
  tasks[,2] = 1:numProfiles; #The targets

  for(round in 1:rounds) {
    tasks[,1] = numProfiles + round; #The (random) regulator

    computationResult = computeAdditiveRegulation(
      deviceSpecs = deviceSpecs, profilesMatrix = profilesWithRandomJava,
      tasks = tasks, constraints = constraints, timeStep = timeStep);

    parameters[[round]] = computationResult$parameters

    fittedProfiles = evaluateAdditiveRegulationResult(computationResult, time, time[1], timeStep);
    numFits = 0;

    for(i in 1:numProfiles) {
      fitQualities[round,i] = fitQuality(profiles[i,], fittedProfiles[i,], errorDef)
    }


    rm(computationResult);
    rm(fittedProfiles);
    gc()
    J("java/lang/System")$gc()
    #cat("Free mem between: ",  (J("java.lang.Runtime")$getRuntime()$maxMemory() - J("java.lang.Runtime")$getRuntime()$totalMemory()) / (1024 * 1024), "MB \n")
  }
  rm(profilesWithRandomJava)
  return(list(fitQualities = fitQualities,
              randomProfiles = randomProfiles,
              parameters = parameters
              ));
}

getTimeStep <- function(time) {
  uniqueDiffTime = unique(diff(time))
  if(length(uniqueDiffTime) > 1) {
    stop("Time must be evenly spaced")
  }
  if(uniqueDiffTime == 1) {
    timeStep = NULL
  } else {
    timeStep = uniqueDiffTime
  }

}

evaluateRandomForRegulon <- function(deviceSpecs, rawProfiles, rounds, regulatorName, regulonNames, time, rawTime, randomScale, randomLength, splineDFs, errorDef = defaultErrorDef(), minFitQuality = 0.8, checkConstantSynthesis = TRUE, splineIntercept = FALSE, constraints = "+") {
  deviceSpecs = getJavaDeviceSpecs(deviceSpecs);

  timeStep = getTimeStep(time)

  profiles = splineProfileMatrix(rawProfiles, rawTime, time, splineDFs, intercept = splineIntercept);

  originalRawProfile = as.numeric(rawProfiles[rownames(rawProfiles) == regulatorName,])

  trueResults = computeRegulon(deviceSpecs, profiles, regulatorName, regulonNames, errorDef = errorDef, minFitQuality = minFitQuality, checkConstantSynthesis = checkConstantSynthesis, constraints = constraints, timeStep = timeStep)


  randomResults = testRandomRegulator(deviceSpecs, rounds = rounds, profiles = profiles[trueResults$tested,], time = time, rawTime = rawTime, randomScale = randomScale, randomLength = randomLength, splineDFs = splineDFs, originalRawProfile =  originalRawProfile, errorDef = errorDef, splineIntercept = splineIntercept, constraints = constraints);

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

testVariousSplines <- function(deviceSpecs, rounds, rawProfiles, rawTime, targetTime, dfsToTest, regulatorName, regulonNames, randomScale, randomLength, errorDef = defaultErrorDef(), minFitQuality = 0.8) {
  results = list();
  deviceSpecs = getJavaDeviceSpecs(deviceSpecs);

  #cat("Free mem before: ",  (J("java.lang.Runtime")$getRuntime()$maxMemory() - J("java.lang.Runtime")$getRuntime()$totalMemory()) / (1024 * 1024), "\n")
  for(i in 1:length(dfsToTest)) {
    currentResult = evaluateRandomForRegulon(
      deviceSpecs, rounds = rounds, rawProfiles = rawProfiles, regulatorName = regulatorName, regulonNames = regulonNames,
      time = targetTime, rawTime = rawTime, randomScale = randomScale, randomLength = randomLength, splineDFs = dfsToTest[i],
      errorDef = errorDef, minFitQuality = minFitQuality)
    results[[i]] = list(df = dfsToTest[i],
                        result = currentResult)
  }
  return(results);
}

f1Helper <- function(numTrue, meanNumFalse, numTested) {
  precision = numTrue / (numTrue + meanNumFalse)
  sensitivity = numTrue / numTested
  return((2 * precision * sensitivity) / (precision + sensitivity))
}

printVariousSplinesResultsHeader <- function() {
  cat("DF\tnumTested\tRegulator\t\tRandom\t\tRatio\n");
}

printVariousSplinesResults <- function(results) {
  for(i in 1:length(results)) {
    numTrue = results[[i]]$result$trueResults$numRegulated
    numTested = results[[i]]$result$trueResults$numTested
    meanNumFalse = results[[i]]$result$overallRandomRatio * numTested
    trueRatio = results[[i]]$result$trueRatio
    randomRatio = results[[i]]$result$overallRandomRatio
    cat(results[[i]]$df, numTested, round(trueRatio, 2), numTrue,
        round(randomRatio, 2),meanNumFalse, trueRatio / randomRatio,  "\n"
        , sep = "\t");
  }
}
