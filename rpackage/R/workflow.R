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
