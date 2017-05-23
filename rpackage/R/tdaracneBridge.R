runTDAracne <- function(profileMatrix, regulatorName, regulonNames, numBins = defaultAracneNumBins) {

  expressionSet = ExpressionSet(assayData = profileMatrix[rownames(profileMatrix) %in% c(regulatorName, regulonNames),]);
  aracneResult = TDARACNE(expressionSet, numBins);

  transClosure = transitive.closure(aracneResult)
  regulatorDownstream = transClosure@edgeL[[regulatorName]]$edges;
  regulatorDownstreamNames = transClosure@nodes[regulatorDownstream];
  regulatorDownstreamNames = regulatorDownstreamNames[regulatorDownstreamNames != regulatorName]

  connectedComps = connectedComp(aracneResult);
  for(i in 1:length(connectedComps)) {
    comp = connectedComps[[i]];
    if(regulatorName %in% comp) {
      connected = comp[comp != regulatorName];
    }
  }

  direct = aracneResult@edgeL[[regulatorName]]$edges;
  directNames = aracneResult@nodes[direct];

  return(list(
    graph = aracneResult,
    direct = directNames,
    downstream = regulatorDownstreamNames,
    connected = connected));
}

testTDAracneRandom <- function(rounds, profileMatrix, time, scale, length, regulatorName, regulonNames, errorDef, numBins) {
  library(foreach);
  library(doParallel);
  cores=detectCores()
  cl <- makeCluster(cores[1] - 1) #not to overload your computer
  registerDoParallel(cl)

  result = array(0, rounds);
  result = foreach(round = 1:rounds) %dopar% {
    randomProfile = generateUsefulRandomProfile(time, scale, length, errorDef, as.numeric(profileMatrix[rownames(profileMatrix) == regulatorName,]))

    customProfileMatrix = profileMatrix;
    customProfileMatrix[rownames(profileMatrix) == regulatorName,] = randomProfile;

    runTDAracne(customProfileMatrix, regulatorName, regulonNames, numBins)
  }
  return(result)

}


evaluateTDAracne <- function(rounds, profileMatrix, time, scale, length, regulatorName, regulonNames, errorDef, numBins) {

  randomResults = testTDAracneRandom(rounds = rounds, profileMatrix = profileMatrix, time = time, scale = scale, length = length, regulatorName = regulatorName, regulonNames = regulonNames, errorDef = errorDef, numBins = numBins)

  trueResults = runTDAracne(profileMatrix, regulatorName, regulonNames)
  numTested = length(trueResults$graph@nodes)
  trueRatioDirect = length(trueResults$direct) / numTested
  trueRatioDownstream = length(trueResults$downstream) / numTested
  trueRatioConnected = length(trueResults$connected) / numTested


  randomRatiosDownstream = sapply(randomResults, FUN = function(x) { length(x$downstream) / numTested })
  randomRatiosConnected = sapply(randomResults, FUN = function(x) { length(x$connected) / numTested })

  return(list(
    trueResults = trueResults,
    trueRatioDirect = trueRatioDirect,
    trueRatioDownstream = trueRatioDownstream,
    trueRatioConnected = trueRatioConnected,
    randomResults = randomResults,
    randomRatiosDownstream = randomRatiosDownstream,
    randomRatiosConnected = randomRatiosConnected,
    overallRandomRatioDownstream = mean(randomRatiosDownstream),
    overallRandomRatioConnected = mean(randomRatiosConnected),
    regulonQuantileDownstream = ecdf(randomRatiosDownstream)(trueRatioDownstream),
    regulonQuantileConnected = ecdf(randomRatiosConnected)(trueRatioConnected)
  ))
}

printTDAracneEvaluation <- function(caption, evaluationResult) {
  cat(paste0(caption," Downstream - True ratio: ", evaluationResult$trueRatioDownstream, " overall random: ", evaluationResult$overallRandomRatioDownstream, "\n"))
  cat(paste0(caption," Connected - True ratio: ", evaluationResult$trueRatioConnected, " overall random: ", evaluationResult$overallRandomRatioConnected, "\n"))
}


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

testTDAracnePairwise <- function(profileMatrix, regulatorName, regulonNames, numBins = defaultAracneNumBins, outputConnection = NULL) {
  numRegulated = 0;
  numTested = 0;
  for(target in regulonNames) {
    if(any(rownames(profileMatrix) == target)) {
      numTested = numTested + 1
      rowStart = paste0(regulatorName, "\t", target, "\t");
      if(testSingleRegulationByTDAracne(profileMatrix, regulatorName, target, numBins)) {
        fullRow = paste0(rowStart, "TRUE\n");
        numRegulated = numRegulated + 1
      }
      else {
        fullRow = paste0(rowStart, "FALSE\n");
      }
      if(~is.null(outputConnection)) {
        cat(fullRow, file = outputConnection);
      }
    }
  }
  return(numRegulated / numTested);
}

testTDAracneRandomPairwise <- function(rounds, profileMatrix, time, scale, length, regulatorName, regulonNames, errorDef = getDefaultErrorDef(), numBins = defaultAracneNumBins) {
  library(foreach);
  library(doParallel);
  cores=detectCores()
  cl <- makeCluster(cores[1] - 1) #not to overload your computer
  registerDoParallel(cl)

  result = array(0, rounds);
  #result = foreach(round = 1:rounds, .combine = cbind) %do% {
  for(round in 1:rounds) {
    randomProfile = generateUsefulRandomProfile(time, scale, length, errorDef, profileMatrix[rownames(profileMatrix) == regulatorName,])

    outputConnection = file(paste0("C:\\Users\\MBU\\Documents\\Genexpi\\genexpi\\rpackage\\test-out\\round_",as.character(round),".csv"), open = "wt");
    customProfileMatrix = profileMatrix;
    customProfileMatrix[rownames(profileMatrix) == regulatorName,] = randomProfile;

    testTDAracne(customProfileMatrix, regulatorName, regulonNames, numBins, outputConnection = outputConnection)
    close(outputConnection)
  }
  return(result)

}

