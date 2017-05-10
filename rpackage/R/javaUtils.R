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
  profileList <- .jcast(.jnew("java/util/ArrayList"), "java/util/List");

  for (p in 1:(dim(profilesMatrix)[1]))
  {
    profileValues <- .jcast(.jnew("java/util/ArrayList"), "java/util/List");
    for( t in 1:dim(profilesMatrix)[2])
    {
      .jcall(profileValues,"Z","add", .jcast(.jnew("java.lang.Float",.jfloat(profilesMatrix[p,t]))));
    }
    newProfile <- .jnew(computeJavaType("GeneProfile"),"", profileValues);
    .jcall(profileList, "Z","add", .jcast(newProfile));
  }

  return(profileList)
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



  processedResult = list(resultsJava = results, parameters = params, errors = errors, profilesMatrix = profilesMatrix, profilesJava = profilesJava, tasks = tasks, tasksJava = tasksJava);
  class(processedResult) <- rClass;
  return(processedResult);
}
