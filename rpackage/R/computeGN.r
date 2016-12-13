#.jcall(result[[1]], "D","getError")
computeGN <- function(profiles, tasks) {

  #TODO: Check parameters
  numRegulators = dim(tasks)[2] - 1;

  profileList <- .jcast(.jnew("java/util/ArrayList"), "java/util/List");

  for (p in 1:(dim(profiles)[1]))
  {
    profileValues <- .jcast(.jnew("java/util/ArrayList"), "java/util/List");
    for( t in 1:dim(profiles)[2])
    {
      .jcall(profileValues,"Z","add", .jcast(.jnew("java.lang.Float",.jfloat(profiles[p,t]))));
    }
    newProfile <- .jnew("cz/cas/mbu/gn/compute/GeneProfile","", profileValues);
    .jcall(profileList, "Z","add", .jcast(newProfile));
  }

  taskListR = list();
  for (i in 1:dim(tasks)[1])
  {
    #Create the inference tasks (move to 0-based indices in Java)
    taskListR[[i]] = .jnew("cz/cas/mbu/gn/compute/AdditiveRegulationInferenceTask", as.integer(tasks[i,1] - 1), as.integer(tasks[i,2] - 1))
  }


  taskListJava <- .jarray(taskListR, contents.class = "cz/cas/mbu/gn/compute/AdditiveRegulationInferenceTask");

  model = .jcall("cz/cas/mbu/gn/compute/InferenceModel","Lcz/cas/mbu/gn/compute/InferenceModel;", "createAdditiveRegulationModel", as.integer(numRegulators));

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
  class(processedResult) <- "gnDifferential";
  #return( .jcall(rInt, "Ljava/util/List", "executeComputation", profileList, taskList));
  return( processedResult );
}


