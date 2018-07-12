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

computeAdditiveRegulation <- function(deviceSpecs, profilesMatrix, tasks, constraints = NULL, numIterations = 256, regularizationWeight = defaultRegularizationWeight(profilesMatrix), timeStep = NULL) {

  #TODO: Check parameters
  numRegulators = dim(tasks)[2] - 1;
  numTasks = dim(tasks)[1];
  if(numRegulators < 1) {
    stop("The given tasks should contain at least one regulator index and one target");
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
  for (i in 1:numTasks)
  {
    #Create the inference tasks (move to 0-based indices in Java)
    regulatorIDs = as.integer((tasks[i,1:numRegulators]) - 1)
    targetID = as.integer(tasks[i,numRegulators + 1] - 1)

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
    if(numRegulators > 1) {
      regulationTypeList = list()
      for(regI in 1:numRegulators) {
        regulationTypeList[[regI]] = regulationType
      }
      regulationType = .jarray(regulationTypeList, contents.class = computeJavaType("RegulationType"))
    }

    taskListR[[i]] = .jnew(computeJavaType("AdditiveRegulationInferenceTask"), regulatorIDs, targetID, regulationType)
  }


  tasksJava <- .jarray(taskListR, contents.class = computeJavaType("AdditiveRegulationInferenceTask"));

  model = J(computeJavaType("InferenceModel"))$createAdditiveRegulationModel(as.integer(numRegulators));

  rInt = rinterfaceJavaType("RInterface");

  if(is.null(timeStep)) {
    useCustomTimeStep = FALSE
    customTimeStep = .jfloat(-1.0)
  } else {
    useCustomTimeStep = TRUE
    customTimeStep = .jfloat(timeStep)
  }

  results = .jcall(rInt,
                   paste0("[L",computeJavaType("InferenceResult"),";"),
                   "computeAdditiveRegulation",
                   getJavaDeviceSpecs(deviceSpecs),
                   profilesJava,
                   tasksJava, model,
                   as.integer(numRegulators),
                   as.integer(numIterations),
                   .jfloat(regularizationWeight),
                   useCustomTimeStep,
                   customTimeStep,
                   evalArray = FALSE
                   );

  return( inferenceResultsToR(results, model, profilesMatrix, profilesJava, tasks, tasksJava, rClass = "additiveRegulationResult"));
}


#Returns a matrix of integrated profiles according to the fit parameters
evaluateAdditiveRegulationResult <- function(additiveRegulationResult, targetTimePoints, initialTime, timeStep) {
  if(class(additiveRegulationResult) != "additiveRegulationResult") {
    stop("Must provide an object of class 'additiveRegulationResult'");
  }
  rInt = rinterfaceJavaType("RInterface");
  results = .jcall(rInt, "[[D", "evaluateAdditiveRegulationResult",
                    additiveRegulationResult$profilesJava,
                    additiveRegulationResult$tasksJava,
                    additiveRegulationResult$model,
                    additiveRegulationResult$resultsJava,
                    as.numeric(initialTime),
                    as.numeric(timeStep),
                    as.numeric(targetTimePoints),
                    evalArray = TRUE,
                    simplify = TRUE
                    );
  return(results);
}

testAdditiveRegulation <- function(additiveRegulationResults, errorDef = defaultErrorDef(), minFitQuality = 0.8, timeStep = NULL) {
  profiles = additiveRegulationResults$profilesMatrix;

  time = (0:(dim(profiles)[2] - 1)) * timeStep;

  numTasks = dim(additiveRegulationResults$tasks)[1]
  additiveRegulationEvaluated = evaluateAdditiveRegulationResult(additiveRegulationResults, time, initialTime = 0, timeStep = timeStep)

  profileNames = character(numTasks)
  isRegulated = logical(dim(profiles)[1])
  regulatedTasks = logical(numTasks)
  for(i in 1:numTasks) {
    #Target is the last column in tasks
    profileIndex = additiveRegulationResults$tasks[i, dim(additiveRegulationResults$tasks)[2]];

    if(fitQuality(profiles[profileIndex,], additiveRegulationEvaluated[i,], errorDef) >= minFitQuality) {
      isRegulated[profileIndex] = TRUE;
      regulatedTasks[i] = TRUE;
    }
    profileNames[i] = rownames(profiles)[profileIndex];
  }
  rownames(additiveRegulationEvaluated) = profileNames

  return(list(regulated = isRegulated, regulatedTasks = regulatedTasks,predictedProfiles = additiveRegulationEvaluated))
}
