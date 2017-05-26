runTDAracne <- function(profileMatrix, regulatorName, regulonNames, numBins = defaultAracneNumBins) {
  library(Biobase)
  library(TDARACNE)
  library(RBGL)

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
  stopCluster(cl)

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
  library(Biobase)
  library(TDARACNE)
  expressionSet = ExpressionSet(assayData = profileMatrix[rownames(profileMatrix) %in% c(regulatorName, targetName),]);

  if(.Platform$OS.type == "windows") {
    sink("NUL")
  } else {
    sink("/dev/null")
  }
  cat("Starting ARACNE\n")
  aracneResult = TDARACNE(expressionSet, numBins);
  sink();

  return(list(
    direct = length(aracneResult@edgeL[[regulatorName]]$edges) > 0,
    reverse = length(aracneResult@edgeL[[targetName]]$edges) > 0))
}

testTDAracnePairwise <- function(profileMatrix, regulatorName, regulonNames, numBins = defaultAracneNumBins, outputConnection = NULL) {
  numRegulatedDirect = 0;
  numRegulatedAny = 0;
  numTested = 0;
  for(target in regulonNames) {
    if(any(rownames(profileMatrix) == target)) {
      numTested = numTested + 1
      rowStart = paste0(regulatorName, "\t", target, "\t");
      singleResult = testSingleRegulationByTDAracne(profileMatrix, regulatorName, target, numBins)
      if(singleResult$direct) {
        fullRow = paste0(rowStart, "TRUE\t");
        numRegulatedDirect = numRegulatedDirect + 1
      }
      else {
        fullRow = paste0(rowStart, "FALSE\t");
      }
      if(singleResult$direct || singleResult$reverse)
      {
        fullRow = paste0(rowStart, "TRUE\n");
        numRegulatedAny = numRegulatedAny + 1
      }
      else {
        fullRow = paste0(rowStart, "FALSE\n");
      }
      if(!is.null(outputConnection)) {
        cat(fullRow, file = outputConnection);
        flush(outputConnection);
      }
    }
  }
  return(list(direct = numRegulatedDirect,
              any = numRegulatedAny,
              numTested = numTested
              ));
}

testTDAracneRandomPairwise <- function(rounds, profileMatrix, time, scale, length, regulatorName, regulonNames, errorDef = getDefaultErrorDef(), numBins = defaultAracneNumBins) {
  library(foreach);
  library(doParallel);
  cores=detectCores()
  cl <- makeCluster(cores[1] - 1) #not to overload your computer
  registerDoParallel(cl)

  result = foreach(round = 1:rounds) %dopar% {
  #result = list();
  #for(round in 1:rounds) {
    randomProfile = generateUsefulRandomProfile(time = time, scale = scale, length = length, errorDef = errorDef, originalProfile = profileMatrix[rownames(profileMatrix) == regulatorName,])

    outputConnection = file(paste0("C:\\Users\\MBU\\Documents\\Genexpi\\genexpi\\rpackage\\test-out\\round_",as.character(round),".csv"), open = "wt");
    customProfileMatrix = profileMatrix;
    customProfileMatrix[rownames(profileMatrix) == regulatorName,] = randomProfile;

    roundResult = testTDAracnePairwise(customProfileMatrix, regulatorName, regulonNames, numBins, outputConnection = outputConnection)
    close(outputConnection)
    #result[[round]] = roundResult;
    return(roundResult)
  }
  stopCluster(cl)
  return(result)

}

evaluateTDAracnePairwise <- function(rounds, profileMatrix, time, scale, length, regulatorName, regulonNames, errorDef, numBins) {

  randomResults = testTDAracneRandomPairwise(rounds = rounds, profileMatrix = profileMatrix, time = time, scale = scale, length = length, regulatorName = regulatorName, regulonNames = regulonNames, errorDef = errorDef, numBins = numBins)

  trueResults = testTDAracnePairwise(profileMatrix, regulatorName, regulonNames)
  numTested = trueResults$numTested
  trueRatioDirect = trueResults$direct / numTested
  trueRatioAny = trueResults$any / numTested

  randomRatiosDirect = sapply(randomResults, FUN = function(x) { x$direct / numTested })
  randomRatiosAny = sapply(randomResults, FUN = function(x) { x$any / numTested })

  return(list(
    trueResults = trueResults,
    trueRatioDirect = trueRatioDirect,
    trueRatioAny = trueRatioAny,
    randomResults = randomResults,
    randomRatiosDirect = randomRatiosDirect,
    randomRatiosAny = randomRatiosAny,
    overallRandomRatioDirect = mean(randomRatiosDirect),
    overallRandomRatioAny = mean(randomRatiosAny),
    regulonQuantileDirect = ecdf(randomRatiosDirect)(trueRatioDirect),
    regulonQuantileAny = ecdf(randomRatiosAny)(trueRatioAny)
  ))
}

printTDAracnePairwiseEvaluation <- function(caption, evaluationResult) {
  cat(paste0(caption," Direct - True ratio: ", evaluationResult$trueRatioDirect, " overall random: ", evaluationResult$overallRandomRatioDirect, "\n"))
  cat(paste0(caption," Any - True ratio: ", evaluationResult$trueRatioAny, " overall random: ", evaluationResult$overallRandomRatioAny, "\n"))
}

