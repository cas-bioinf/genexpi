#Measures performance of a set of random profiles in explaining the profiles in regulon
testRandomRegulator <- function(deviceSpecs, rounds, profiles, time, randomScale, randomLength, rawTime, originalRawProfile, splineDFs, errorDef = defaultErrorDef()) {
  profilesDim = dim(profiles);
  numProfiles = profilesDim[1];
  numTime = profilesDim[2];

  fitQualities = array(0, c(rounds, numProfiles));

  randomProfilesRaw = array(0, c(rounds, length(rawTime)));
  for(round in 1:rounds) {
    randomProfilesRaw[round,] = generateUsefulRandomProfile(rawTime, randomScale, randomLength, errorDef, originalRawProfile)
  }
  randomProfiles = splineProfileMatrix(randomProfilesRaw, rawTime, time, splineDFs);

  #Cache the java object and reuse it
  profilesWithRandomJava = geneProfilesFromMatrix(rbind(profiles, randomProfiles))

  #The first element (regulator) of tasks changes, the second stays the same
  tasks = array(0, c(numProfiles,2));
  tasks[,2] = 1:numProfiles; #The targets

  #Force only positive interactions
  constraints = "+";

  for(round in 1:rounds) {
    tasks[,1] = numProfiles + round; #The (random) regulator

    computationResult = computeAdditiveRegulation(
      deviceSpecs = deviceSpecs, profilesMatrix = profilesWithRandomJava,
      tasks = tasks, constraints = constraints);
    fittedProfiles = evaluateAdditiveRegulationResult(computationResult, time);
    numFits = 0;
    for(i in 1:numProfiles) {
      fitQualities[round,i] = fitQuality(profiles[i,], fittedProfiles[i,], errorDef)
    }
    rm(computationResult);
    rm(fittedProfiles);
    gc()
    #cat("Free mem between: ",  (J("java.lang.Runtime")$getRuntime()$maxMemory() - J("java.lang.Runtime")$getRuntime()$totalMemory()) / (1024 * 1024), "\n")
  }
  return(list(fitQualities = fitQualities,
              randomProfiles = randomProfiles));
}

evaluateRandomForRegulon <- function(deviceSpecs, rawProfiles, rounds, regulatorName, regulonNames, time, rawTime, randomScale, randomLength, splineDFs, errorDef = defaultErrorDef(), minFitQuality = 0.8) {
  profiles = splineProfileMatrix(rawProfiles, rawTime, time, splineDFs);

  originalRawProfile = as.numeric(rawProfiles[rownames(rawProfiles) == regulatorName,])

  trueResults = computeRegulon(deviceSpecs, profiles, regulatorName, regulonNames, errorDef = errorDef, minFitQuality = minFitQuality)


  randomResults = testRandomRegulator(deviceSpecs, rounds = rounds, profiles = profiles[trueResults$tested,], time = time, rawTime = rawTime, randomScale = randomScale, randomLength = randomLength, splineDFs = splineDFs, originalRawProfile =  originalRawProfile, errorDef = errorDef);

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
  cat("DF\tType\tnumTested\tRegulator\t\tRandom\t\n");
}

printVariousSplinesResults <- function(results) {
  for(i in 1:length(results)) {
    numTrue = results[[i]]$result$trueResults$numRegulated
    numTested = results[[i]]$result$trueResults$numTested
    meanNumFalse = results[[i]]$result$overallRandomRatio * numTested
    cat(results[[i]]$df, "Genexpi", numTested, results[[i]]$result$trueRatio, numTrue,
        results[[i]]$result$overallRandomRatio,meanNumFalse, "\n"
        , sep = "\t");
  }
}
