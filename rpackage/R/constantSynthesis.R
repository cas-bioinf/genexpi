#Returns a boolean matrix. True -> can be fit by a constant profile
testConstant <- function(profilesMatrix, errorDef = defaultErrorDef() ) {
  errors = errorMargin(profilesMatrix, errorDef);
  minUpperBound = apply(profilesMatrix + errors, FUN = min, MARGIN = 1)
  maxLowerBound = apply(profilesMatrix - errors, FUN = max, MARGIN = 1)
  return(minUpperBound > maxLowerBound)
}

computeConstantSynthesis <- function(deviceSpecs, profilesMatrix, tasks = NULL, numIterations = 128) {
  if(is.null(tasks)) {
    tasks = 1:(dim(profilesMatrix)[1]);
  }

  taskListR = list();
  for (i in 1:length(tasks))
  {
    #Create the inference tasks (move to 0-based indices in Java)
    taskListR[[i]] = .jnew(computeJavaType("NoRegulatorInferenceTask"), as.integer(tasks[i] - 1))
  }

  tasksJava <- .jarray(taskListR, contents.class = computeJavaType("NoRegulatorInferenceTask"));

  profilesJava = geneProfilesFromMatrix(profilesMatrix)

  rInt = rinterfaceJavaType("RInterface");
  results = .jcall(rInt, paste0("[L",computeJavaType("InferenceResult"),";"), "computeConstantSynthesis", deviceSpecs, profilesJava, tasksJava, as.integer(numIterations), evalArray = FALSE);

  model = J(computeJavaType("InferenceModel"))$NO_REGULATOR;

  return( inferenceResultsToR(results, model, profilesMatrix, profilesJava, tasks, tasksJava, rClass = "constantSynthesisResult"));
}

evaluateConstantSynthesis <- function(constantSynthesisResult, targetTimePoints) {
  if(class(constantSynthesisResult) != "constantSynthesisResult") {
    error("Must provide an object of class 'constantSynthesisResult'");
  }
  rInt = rinterfaceJavaType("RInterface");
  results = J(rInt)$evaluateConstantSynthesisResult(constantSynthesisResult$profilesJava,
                                          constantSynthesisResult$tasksJava,
                                          constantSynthesisResult$resultsJava,
                                          targetTimePoints);
  # results = .jcall(rInt, "[[D", "evaluateConstantSynthesisResult",
  #                  constantSynthesisResult$profilesJava,
  #                  constantSynthesisResult$tasksJava,
  #                  constantSynthesisResult$resultsJava,
  #                  targetTimePoints);
  return(.jevalArray(results, simplify = TRUE));
}

testConstantSynthesis <- function(constantSynthesisResults, errorDef = defaultErrorDef(), minFitQuality = 0.8 ) {

}
