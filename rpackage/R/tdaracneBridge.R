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

testTDAracneRandom <- function(rounds, profileMatrix, time, rawTime, splineDFs, randomScale, randomLength, regulatorName, regulonNames, originalProfileRaw, errorDef, numBins) {
  library(foreach);
  library(doParallel);
  cores=detectCores()
  cl <- makeCluster(cores[1] - 1) #not to overload your computer
  registerDoParallel(cl)

  result = array(0, rounds);
  result = foreach(round = 1:rounds) %dopar% {
    randomProfileRaw = generateUsefulRandomProfile(rawTime, randomScale, randomLength, errorDef, as.numeric(originalProfileRaw))

    if(is.null(splineDFs)) {
      randomProfile = randomProfileRaw
    }
    else {
      randomProfile = splineProfileMatrix(randomProfileRaw, rawTime, time, splineDFs)
    }

    customProfileMatrix = profileMatrix;
    customProfileMatrix[rownames(profileMatrix) == regulatorName,] = randomProfile;

    runTDAracne(customProfileMatrix, regulatorName, regulonNames, numBins)
  }
  stopCluster(cl)

  return(result)

}


evaluateTDAracne <- function(rounds, profilesRaw, rawTime, splineDFs, time, randomScale, randomLength, regulatorName, regulonNames, errorDef, numBins) {

  if(is.null(splineDFs))
  {
    profileMatrix = profilesRaw
    time = rawTime
  }
  else {
    profileMatrix = splineProfileMatrix(profilesRaw, rawTime, time, splineDFs)
  }

  randomResults = testTDAracneRandom(
    rounds = rounds, profileMatrix = profileMatrix, time = time, rawTime = rawTime, splineDFs = splineDFs,
    randomScale = randomScale, randomLength = randomLength, regulatorName = regulatorName, regulonNames = regulonNames,
    originalProfileRaw = profilesRaw[rownames(profilesRaw) == regulatorName,],
    errorDef = errorDef, numBins = numBins)

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

printTDAracneEvaluationHeader <- function() {
  cat("DF\tType\tnumTested\tRegulator\t\tRandom\t\n");
}

printTDAracneEvaluationRow <- function(df, type, trueRatio, randomRatio, numTested) {
  cat(df,type, numTested,round(trueRatio,2), round(numTested*trueRatio),
      round(randomRatio, 2), numTested*randomRatio, sep = "\t")
  cat("\n")
}

printTDAracneEvaluation <- function(df, evaluationResult, genexpiResult) {
  testedByGenexpi = genexpiResult$result$trueResults$tested;
  genexpiTestedNames = rownames(genexpiResult$result$trueResults$regulationResults$profilesMatrix)[testedByGenexpi]

  numTestedAracne = length(evaluationResult$trueResults$graph@nodes)
  numTestedBoth = sum(genexpiTestedNames %in% evaluationResult$trueResults$graph@nodes);

  trueDownstreamNames = evaluationResult$trueResults$downstream
  trueConnectedNames = evaluationResult$trueResults$connected

  numTrueDownstreamLimited = sum(genexpiTestedNames %in% trueDownstreamNames)
  numTrueConnectedLimited = sum(genexpiTestedNames %in% trueConnectedNames)

  meanNumFalseDownstreamLimited = mean(sapply(evaluationResult$randomResults, FUN = function(x) { sum(x$downstream %in% genexpiTestedNames) }))
  meanNumFalseConnectedLimited = mean(sapply(evaluationResult$randomResults, FUN = function(x) { sum(x$connected %in%  genexpiTestedNames) }))

  #downstreamF1 = f1Helper(evaluationResult$trueRatioDownstream * numTested, evaluationResult$overallRandomRatioDownstream * numTested, numTested)
  #connectedF1 = f1Helper(evaluationResult$trueRatioConnected * numTested, evaluationResult$overallRandomRatioConnected * numTested, numTested)
  printTDAracneEvaluationRow(df, "Downstream  ", evaluationResult$trueRatioDownstream, evaluationResult$overallRandomRatioDownstream,numTestedAracne)
  printTDAracneEvaluationRow(df, "Connected   ", evaluationResult$trueRatioConnected, evaluationResult$overallRandomRatioConnected, numTestedAracne)
  printTDAracneEvaluationRow(df, "Downstream-L", numTrueDownstreamLimited / numTestedBoth, meanNumFalseDownstreamLimited / numTestedBoth,numTestedBoth)
  printTDAracneEvaluationRow(df, "Connected-L ", numTrueConnectedLimited / numTestedBoth, meanNumFalseConnectedLimited / numTestedBoth,numTestedBoth)
}


testSingleRegulationByTDAracne <- function(profileMatrix, regulatorName, targetName, numBins = defaultAracneNumBins) {
  library(Biobase)
  library(TDARACNE)
  profileSubset = profileMatrix[rownames(profileMatrix) %in% c(regulatorName, targetName),, drop = FALSE];
  expressionSet = ExpressionSet(assayData = profileSubset);

  if(dim(profileSubset)[1] != 2) {
    stop(paste0("Couldn't find some of the requested genes. Target: ", targetName, " regulator: ", regulatorName))
  }

  if(.Platform$OS.type == "windows") {
    sink("NUL")
  } else {
    sink("/dev/null")
  }
  aracneResult = TDARACNE(expressionSet, numBins);
  sink();

  return(list(
    direct = length(aracneResult@edgeL[[regulatorName]]$edges) > 0,
    reverse = length(aracneResult@edgeL[[targetName]]$edges) > 0))
}

testTDAracnePairwise <- function(profileMatrix, regulatorName, regulonNames, numBins = defaultAracneNumBins, outputConnection = NULL) {

  numProfiles = dim(profileMatrix)[1]
  tested = logical(numProfiles)
  regulatedDirect = logical(numProfiles)
  regulatedAny = logical(numProfiles)

  for(i in 1:numProfiles) {
    target = rownames(profileMatrix)[i]
    if(target %in% regulonNames) {
      tested[i] = TRUE;
      rowStart = paste0(regulatorName, "\t", target, "\t");
      singleResult = testSingleRegulationByTDAracne(profileMatrix, regulatorName, target, numBins)
      if(singleResult$direct) {
        fullRow = paste0(rowStart, "TRUE\t");
        regulatedDirect[i] = TRUE;
      }
      else {
        fullRow = paste0(rowStart, "FALSE\t");
        regulatedDirect[i] = FALSE;
      }
      if(singleResult$direct || singleResult$reverse)
      {
        fullRow = paste0(fullRow, "TRUE\n");
        regulatedAny[i] = TRUE;
      }
      else {
        fullRow = paste0(fullRow, "FALSE\n");
        regulatedAny[i] = FALSE;
      }
      if(!is.null(outputConnection)) {
        cat(fullRow, file = outputConnection);
        flush(outputConnection);
      }
    }
    else {
      tested[i] = FALSE;
      regulatedAny[i] = FALSE;
      regulatedDirect[i] = FALSE
    }
  }
  return(list(direct = regulatedDirect,
              any = regulatedAny,
              tested = tested
              ));
}

testTDAracneRandomPairwise <- function(title, rounds, profileMatrix, time, rawTime, splineDFs, randomScale,
                                       randomLength, regulatorName, regulonNames, originalProfileRaw,
                                       errorDef = getDefaultErrorDef(), numBins = defaultAracneNumBins) {
  library(foreach);
  library(doParallel);
  cores=detectCores()
  cl <- makeCluster(cores[1] - 1) #not to overload your computer
  registerDoParallel(cl)

  #result = foreach(round = 1:rounds) %dopar% {
  result = list();
  for(round in 1:rounds) {
    randomProfileRaw = generateUsefulRandomProfile(time = rawTime, scale = randomScale, length = randomLength, errorDef = errorDef, originalProfile = originalProfileRaw)
    if(is.null(splineDFs)) {
      randomProfile = randomProfileRaw
    }
    else {
      randomProfile = splineProfileMatrix(randomProfileRaw, rawTime, time, splineDFs)
    }

    #outputConnection = file(paste0("C:\\Users\\MBU\\Documents\\Genexpi\\genexpi\\rpackage\\test-out\\",title,"_round_",as.character(round),".csv"), open = "wt");
    outputConnection = NULL;
    customProfileMatrix = profileMatrix;
    customProfileMatrix[rownames(profileMatrix) == regulatorName,] = randomProfile;

    roundResult = testTDAracnePairwise(customProfileMatrix, regulatorName, regulonNames, numBins, outputConnection = outputConnection)
    if(!is.null(outputConnection)) {
      close(outputConnection)
    }
    result[[round]] = roundResult;
    #return(roundResult)
  }
  stopCluster(cl)
  return(result)

}

evaluateTDAracnePairwise <- function(title, rounds, profilesRaw, time, rawTime, splineDFs, randomScale, randomLength, regulatorName, regulonNames, errorDef, numBins) {
  originalProfileRaw = profilesRaw[rownames(profilesRaw) == regulatorName,]
  if(is.null(splineDFs)) {
    profileMatrix = profilesRaw
    time = rawTime
  }
  else {
    profileMatrix = splineProfileMatrix(profilesRaw, rawTime, time, splineDFs)
  }

  randomResults = testTDAracneRandomPairwise(
    title = title, rounds = rounds, profileMatrix = profileMatrix, time = time, rawTime = rawTime,
    randomScale = randomScale, randomLength = randomLength, regulatorName = regulatorName,
    originalProfileRaw = originalProfileRaw, splineDFs = splineDFs,
    regulonNames = regulonNames, errorDef = errorDef, numBins = numBins)

  trueResults = testTDAracnePairwise(profileMatrix, regulatorName, regulonNames, numBins = numBins)
  numTested = sum(trueResults$tested)
  trueRatioDirect = sum(trueResults$direct) / numTested
  trueRatioAny = sum(trueResults$any) / numTested

  randomRatiosDirect = sapply(randomResults, FUN = function(x) { sum(x$direct) / numTested })
  randomRatiosAny = sapply(randomResults, FUN = function(x) { sum(x$any) / numTested })

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

printTDAracnePairwiseEvaluation <- function(df, evaluationResult, genexpiResult) {
  testedByGenexpi = genexpiResult$result$trueResults$tested;
  numTestedAracne = sum(evaluationResult$trueResults$tested)
  numTestedBoth = sum(testedByGenexpi & evaluationResult$trueResults$tested);

  numTrueAracneDirect = sum(evaluationResult$trueResults$direct)
  numTrueAracneAny = sum(evaluationResult$trueResults$any)

  meanNumFalseDirect = mean(sapply(evaluationResult$randomResults, FUN = function(x) { sum(x$direct) }))
  meanNumFalseAny = mean(sapply(evaluationResult$randomResults, FUN = function(x) { sum(x$any) }))

  numTrueDirectTested = sum(evaluationResult$trueResults$direct & testedByGenexpi)
  numTrueAnyTested = sum(evaluationResult$trueResults$any & testedByGenexpi)

  trueRatioDirectTested =  numTrueDirectTested / numTestedBoth
  trueRatioAnyTested =  numTrueAnyTested / numTestedBoth

  meanNumFalseDirectTested = mean(sapply(evaluationResult$randomResults, FUN = function(x) { sum(x$direct & testedByGenexpi) }))
  meanNumFalseAnyTested = mean(sapply(evaluationResult$randomResults, FUN = function(x) { sum(x$any & testedByGenexpi) }))

  falseRatioDirectTested = meanNumFalseDirectTested / numTestedBoth
  falseRatioAnyTested = meanNumFalseAnyTested / numTestedBoth

  directF1 = f1Helper(numTrueAracneDirect, meanNumFalseDirect, numTestedAracne)
  anyF1 = f1Helper(numTrueAracneAny, meanNumFalseAny, numTestedAracne)
  directTestedF1 = f1Helper(numTrueDirectTested, meanNumFalseDirectTested, numTestedBoth)
  anyTestedF1 = f1Helper(numTrueAnyTested, meanNumFalseAnyTested, numTestedBoth)

  printTDAracneEvaluationRow(df,"Direct  ",evaluationResult$trueRatioDirect, evaluationResult$overallRandomRatioDirect, numTestedAracne);
  printTDAracneEvaluationRow(df,"Any     ",evaluationResult$trueRatioAny, evaluationResult$overallRandomRatioAny, numTestedAracne);
  printTDAracneEvaluationRow(df,"Direct-L",trueRatioDirectTested, falseRatioDirectTested, numTestedBoth);
  printTDAracneEvaluationRow(df,"Any-L   ",trueRatioAnyTested, falseRatioAnyTested, numTestedBoth);
}

