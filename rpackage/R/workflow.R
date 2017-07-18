splineProfileMatrix <- function(profileMatrix, time, targetTime, df, degree = 3) {
  if(any(is.na(profileMatrix))) {
    stop("Profiles cannot be NA")
  }
  #Create the spline basis
  splineBasis = bs(time, df = df,degree = degree)
  #Use a least-squares fit of a B-spline basis
  splineFit <- lm(t(profileMatrix) ~ 0 + splineBasis); #Without intercept

  #Create the same basis but for the target time
  basisNew = bs(x = targetTime, degree = degree, knots = attr(splineBasis, "knots"), Boundary.knots = attr(splineBasis, "Boundary.knots"));

  #The result is the product of the spline coefficients with the spline basis for target time
  splinedResult = t(basisNew %*% splineFit$coefficients);

  #Ensure the profiles are strictly positive
  splinedResult[splinedResult < 0] = 0;

  rownames(splinedResult) = rownames(profileMatrix)
  return(splinedResult)
}

inspectSmoothing <- function(timeRaw, profilesRaw, timeSmooth, profilesSmooth, genes) {
  colors = c("orange","blue", "green","black","magenta")
  if(length(genes) > length(colors)) {
    stop("Too many genes to inspect.")
  }

  ylim = c(0, max(max(profilesRaw[genes,]), max(profilesSmooth[genes,])))
  plot(1, type="n", xlab="", ylab="", xlim=c(min(timeSmooth), max(timeSmooth)), ylim=ylim)
  for(i in 1:length(genes)) {
    lines(timeRaw, profilesRaw[genes[i],], col = colors[i],lty = 4)
    lines(timeSmooth, profilesSmooth[genes[i],], col = colors[i])
    cat(genes[i],"-",colors[i],"\n")
  }
}


computeRegulon <- function(deviceSpecs, profiles, regulatorName, regulonNames, errorDef = defaultErrorDef(), minFitQuality = 0.8) {
  if(is.null(rownames(profiles))) {
    stop("Profiles must have associated rownames");
  }
  targetProfiles = rownames(profiles) %in% regulonNames

  deviceSpecs = getJavaDeviceSpecs(deviceSpecs);

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

      regulationResults = computeAdditiveRegulation(deviceSpecs, profiles, tasks, constraints = "+")
      testResults = testAdditiveRegulation(regulationResults, errorDef, minFitQuality)
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
    predictedProfiles = testResults$predictedProfiles,
    numRegulated = sum(testResults$regulated),
    regulated = testResults$regulated,
    regulatorName = regulatorName,
    regulonNames = regulonNames,
    errorDef = errorDef,
    minFitQuality = minFitQuality
  ))
}

inspectRegulonFit <- function(regulonResult, targetGene) {
  if(!require("ggplot2",character.only = TRUE)) stop("inspectFit requires ggplot2");

  targetProfile = regulonResult$regulationResults$profilesMatrix[targetGene,];
  err = errorMargin(targetProfile, regulonResult$errorDef)
  targetErrorMax = targetProfile + err;
  targetErrorMin = targetProfile - err;
  targetErrorMin[targetErrorMin < 0] = 0;

  targetLabel = paste(targetGene, " - Data")
  predictedLabel = paste(targetGene, " - Predicted")

  data = data.frame(
    time = 1:length(targetProfile),
    target = targetProfile,
    regulator = regulonResult$regulationResults$profilesMatrix[regulonResult$regulatorName,],
    predicted = regulonResult$predictedProfiles[targetGene,],
    targetErrorMin = targetErrorMin,
    targetErrorMax = targetErrorMax
  )

  colorMap = c("red","blue", "green")
  names(colorMap) = c(targetLabel, regulonResult$regulatorName, predictedLabel)

  ggplot(data, aes(x=time)) +
    geom_ribbon(aes(ymin = targetErrorMin, ymax = targetErrorMax), fill = "red", alpha = 0.2) +
    geom_line(aes(y = target, colour = targetLabel)) +
    geom_line(aes(y = regulator, colour = regulonResult$regulatorName)) +
    geom_line(aes(y = predicted, colour = predictedLabel)) +
    scale_colour_manual("", values = colorMap)
}
