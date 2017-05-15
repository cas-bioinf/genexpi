rinterfaceJavaType <- function (typeName) {
  return (paste0("cz/cas/mbu/genexpi/rinterface/", (gsub("\\.","/", typeName))));
}

rinterfaceJavaReturnType <- function(typeName) {
  return (paste0("L", rinterfaceJavaType(typeName), ";"));
}

computeJavaType <- function (typeName) {
  return (paste0("cz/cas/mbu/genexpi/compute/", (gsub("\\.","/", typeName))));
}

computeJavaReturnType <- function(typeName) {
  return (paste0("L", computeJavaType(typeName), ";"));
}


geneProfilesFromMatrix <- function(profilesMatrix) {
  matrixForJava = .jarray(profilesMatrix, dispatch = TRUE);
  geneNames = rownames(profilesMatrix);
  if(is.null(geneNames)) {
    geneNames = as.character(1:dim(profilesMatrix)[1]);
  }
  return(.jcall(rinterfaceJavaType("RInterface"), "Ljava/util/List;", "geneProfilesFromMatrix", matrixForJava, geneNames));
}

inferenceResultsToR <- function(results, model, profilesMatrix, profilesJava, tasks, tasksJava, rClass) {
  numParams = .jcall(model, "I", "getNumParams")
  params = matrix(-1,nrow = length(results), ncol = numParams)
  colnames(params) <- .jcall(model,"[S", "getParameterNames")
  errors = numeric(length(results))

  for (i in 1:length(results))
  {
    params[i,] = .jcall(results[[i]],"[D","getParameters");
    errors[i] = .jcall(results[[i]], "D","getError");
  }



  processedResult = list(resultsJava = results, model = model, parameters = params, errors = errors, profilesMatrix = profilesMatrix, profilesJava = profilesJava, tasks = tasks, tasksJava = tasksJava);
  class(processedResult) <- rClass;
  return(processedResult);
}
