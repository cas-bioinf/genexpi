defaultAracneNumBins = 10

testSingleRegulationByTDAracne <- function(profileMatrix, regulatorName, targetName, numBins = defaultAracneNumBins) {
  expressionSet = ExpressionSet(assayData = profileMatrix[rownames(profileMatrix) %in% c(regulatorName, targetName),]);

  if(.Platform$OS.type == "windows") {
    sink("NUL")
  } else {
    sink("/dev/null")
  }
  aracneResult = TDARACNE(expressionSet, numBins);
  sink();

  return(length(aracneResult@edgeL[[regulatorName]]$edges) > 0 || length(aracneResult@edgeL[[targetName]]$edges) > 0);
}

testTDAracne <- function(profileMatrix, regulatorName, regulonNames, numBins = defaultAracneNumBins) {
  numRegulated = 0;
  numTested = 0;
  for(target in regulonNames) {
    if(any(rownames(profileMatrix) == target)) {
      numTested = numTested + 1
      if(testSingleRegulationByTDAracne(profileMatrix, regulatorName, target, numBins)) {
        numRegulated = numRegulated + 1
      }
    }
  }
  return(numRegulated / numTested);
}

testTDAracneRandom <- function(rounds, profileMatrix, time, scale, length, regulatorName, regulonNames, errorDef = getDefaultErrorDef(), numBins = defaultAracneNumBins) {
  result = array(0, rounds);
  for(round in 1:rounds ) {
    randomProfile = generateUsefulRandomProfile(time, scale, length, errorDef, profileMatrix[rownames(profileMatrix) == regulatorName])

    profileMatrix[rownames(profileMatrix) == regulatorName,] = randomProfile;
    result[round] = testTDAracne(profileMatrix, regulatorName, regulonNames, numBins);
  }
  return(result)

}

