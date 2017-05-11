computeAdditiveRegulation <- function(deviceSpecs, profilesMatrix, tasks, constraints = NULL, numIterations = 128, regularizationWeight = dim(profilesMatrix)[2] / 10) {

  #TODO: Check parameters
  numRegulators = dim(tasks)[2] - 1;
  if(numRegulators < 1) {
    stop("The given tasks should contain at least one regulator index and one target");
  }
  if(numRegulators > 1) {
    stop("More than one regulator currently not supported");
  }

  profilesJava = geneProfilesFromMatrix(profilesMatrix)

  taskListR = list();
  for (i in 1:dim(tasks)[1])
  {
    #TODO allow multiple regulators
    #Create the inference tasks (move to 0-based indices in Java)
    taskListR[[i]] = .jnew(computeJavaType("AdditiveRegulationInferenceTask"), as.integer(tasks[i,1] - 1), as.integer(tasks[i,2] - 1))
  }


  tasksJava <- .jarray(taskListR, contents.class = computeJavaType("AdditiveRegulationInferenceTask"));

  model = J(computeJavaType("InferenceModel"))$createAdditiveRegulationModel(as.integer(numRegulators));

  #TODO: Constraints

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
