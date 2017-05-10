computeAdditiveRegulation <- function(deviceSpecs, profilesMatrix, tasks, constraints = NULL) {

  #TODO: Check parameters
  numRegulators = dim(tasks)[2] - 1;
  if(numRegulators < 1) {
    stop("The given tasks should contain at least one regulator index and one target");
  }

  profileList = geneProfilesFromMatrix(profilesMatrix)

  taskListR = list();
  for (i in 1:dim(tasks)[1])
  {
    #TODO allow multiple regulators
    #Create the inference tasks (move to 0-based indices in Java)
    taskListR[[i]] = .jnew("cz/cas/mbu/gn/compute/AdditiveRegulationInferenceTask", as.integer(tasks[i,1] - 1), as.integer(tasks[i,2] - 1))
  }


  taskListJava <- .jarray(taskListR, contents.class = "cz/cas/mbu/gn/compute/AdditiveRegulationInferenceTask");

  model = .jcall("cz/cas/mbu/gn/compute/InferenceModel","Lcz/cas/mbu/gn/compute/InferenceModel;", "createAdditiveRegulationModel", as.integer(numRegulators));

  #TODO: Constraints

  rInt = .jnew("cz/cas/mbu/gn/rinterface/RInterface");
  result = .jcall(rInt, "[Lcz/cas/mbu/gn/compute/InferenceResult;", "executeComputation", profileList, taskListJava, model);


  numParams = .jcall(model, "I", "getNumParams")
  params = matrix(-1,nrow = length(result), ncol = numParams)
  colnames(params) <- .jcall(model,"[S", "getParameterNames")
  errors = numeric(length(result))

  for (i in 1:length(result))
  {
    params[i,] = .jcall(result[[i]],"[D","getParameters");
    errors[i] = .jcall(result[[i]], "D","getError");
  }



  processedResult = list(parameters = params, errors = errors, profiles = profiles, tasks = tasks);
  class(processedResult) <- "additiveRegulationResult";
  return( processedResult );
}


#Returns a matrix of integrated profiles according to the fit parameters
evaluateAdditiveRegulationResult <- function(additiveRegulationResult) {

}
