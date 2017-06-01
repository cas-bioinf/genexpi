defaultRegularizationWeight <- function(profilesMatrix) {
  if(class(profilesMatrix) ==  "jobjRef") {
    if(profilesMatrix$isEmpty()) {
      return(0);
    }
    else {
      return(profilesMatrix$get(as.integer(0))$getProfile()$size() / 10);
    }
  }
  else {
    return(dim(profilesMatrix)[2] / 10)
  }
}

computeAdditiveRegulation <- function(deviceSpecs, profilesMatrix, tasks, constraints = NULL, numIterations = 128, regularizationWeight = defaultRegularizationWeight(profilesMatrix)) {

  #TODO: Check parameters
  numRegulators = dim(tasks)[2] - 1;
  if(numRegulators < 1) {
    stop("The given tasks should contain at least one regulator index and one target");
  }
  if(numRegulators > 1) {
    stop("More than one regulator currently not supported");
  }

  if(class(profilesMatrix) ==  "jobjRef") {
    profilesJava = profilesMatrix
  } else {
    profilesJava = geneProfilesFromMatrix(profilesMatrix)
  }

  if(is.null(constraints)) {
    constraints = array("", dim(tasks)[1])
  } else if(length(constraints) == 1) {
    constraints = array(constraints, dim(tasks)[1])
  }


  taskListR = list();
  for (i in 1:dim(tasks)[1])
  {
    #TODO allow multiple regulators
    #Create the inference tasks (move to 0-based indices in Java)
    if(constraints[i] == "")
    {
      regulationType = J(computeJavaType("RegulationType"))$All;
    }
    else if(constraints[i] == "+") {
      regulationType = J(computeJavaType("RegulationType"))$PositiveOnly;
    }
    else if(constraints[i] == "-") {
      regulationType = J(computeJavaType("RegulationType"))$NegativeOnly;
    }
    else {
      stop(paste0("Unknown constraint: ", constraints[i]));
    }

    taskListR[[i]] = .jnew(computeJavaType("AdditiveRegulationInferenceTask"), as.integer(tasks[i,1] - 1), as.integer(tasks[i,2] - 1), regulationType)
  }


  tasksJava <- .jarray(taskListR, contents.class = computeJavaType("AdditiveRegulationInferenceTask"));

  model = J(computeJavaType("InferenceModel"))$createAdditiveRegulationModel(as.integer(numRegulators));

  rInt = rinterfaceJavaType("RInterface");
  results = .jcall(rInt, paste0("[L",computeJavaType("InferenceResult"),";"), "computeAdditiveRegulation", deviceSpecs, profilesJava, tasksJava, model, as.integer(numIterations), .jfloat(regularizationWeight), evalArray = FALSE);

  return( inferenceResultsToR(results, model, profilesMatrix, profilesJava, tasks, tasksJava, rClass = "additiveRegulationResult"));
}


#Returns a matrix of integrated profiles according to the fit parameters
evaluateAdditiveRegulationResult <- function(additiveRegulationResult, targetTimePoints) {
  if(class(additiveRegulationResult) != "additiveRegulationResult") {
    stop("Must provide an object of class 'additiveRegulationResult'");
  }
  rInt = rinterfaceJavaType("RInterface");
  results = .jcall(rInt, "[[D", "evaluateAdditiveRegulationResult",
                    additiveRegulationResult$profilesJava,
                    additiveRegulationResult$tasksJava,
                    additiveRegulationResult$model,
                    additiveRegulationResult$resultsJava,
                    as.numeric(targetTimePoints),
                    evalArray = TRUE,
                    simplify = TRUE
#                    check = FALSE
                    );
 # ex =.jgetEx();
  #.jclear();
  #ex$printStackTrace();
  return(results);
}

testAdditiveRegulation <- function(additiveRegulationResults, errorDef = defaultErrorDef(), minFitQuality = 0.8 ) {
  profiles = additiveRegulationResults$profilesMatrix;

  time = 0:(dim(profiles)[2] - 1);

  additiveRegulationEvaluated = evaluateAdditiveRegulationResult(additiveRegulationResults, time)

  regulatedProfiles = logical(dim(profiles)[1])
  for(i in 1:dim(additiveRegulationResults$tasks)[1]) {
    #Target is the last column in tasks
    profileIndex = additiveRegulationResults$tasks[i, dim(additiveRegulationResults$tasks)[2]];

    if(fitQuality(profiles[profileIndex,], additiveRegulationEvaluated[i,], errorDef) >= minFitQuality) {
      regulatedProfiles[profileIndex] = TRUE;
    }
  }

  return(regulatedProfiles)
}
