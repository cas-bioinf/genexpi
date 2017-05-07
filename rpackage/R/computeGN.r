getDeviceSpecs <- function(device = NULL, deviceType = NULL) {
    if(is.null(device)) {
      if (is.null(deviceType)) {
        return (.jnew(rinterfaceType("DeviceSpecs")));

      }
      else {
        if (deviceType == "gpu") {
          return (.jcall(rinterfaceType("DeviceSpecs"), rinterfaceReturnType("DeviceSpecs"), "gpuSpecs"))
        }
        else if (deviceType == "processor") {
          return (.jcall(rinterfaceType("DeviceSpecs"), rinterfaceReturnType("DeviceSpecs"), "processorSpecs"))
        }
        else {
          stop("deviceType has to be either 'gpu' or 'processor'");
        }

      }
    }
    else {
      if(!is.null(deviceType)) {
        stop("You cannot specify both device and deviceType");
      }

      deviceList = J(rinterfaceType("RInterface"))$getAllDevices();
      numDevices = deviceList$size();
      if(is.integer(device)) {
        if(device < 0 || device >= numDevices) {
          stop("Invalid device ID. Call XXX to get the list of possible devices"); #TODO method name
        }
        return (new(J(rinterfaceType("DeviceSpecs"), deviceList$get(device))));
      }
    }

}

defaultErrorDef <- function() {
  return(list(absolute = 0, relative = 0.2, minimal = 0))
}

errorMargin <- function(x, errorDef = defaultErrorDef()) {
  errors = x * errorDef$relative + errorDef$absolute;
  errors[errors < errorDef$minimal] = errorDef$minimal;
  return(errors)
}

profileFit <- function(targetProfile, candidateProfile, errorDef = defaultErrorDef()) {
  if(length(targetProfile) != length(candidateProfile)) {
    error("Profiles must have the same length")
  }
  matches = abs(targetProfile - candidateProfile) < errorMargin(targetProfile, errorDef);
  return(mean(matches));
}

#Returns a boolean matrix. True -> can be fit by a constant profile
testConstant <- function(profiles, errorDef = defaultErrorDef() ) {
  errors = errorMargin(profiles, errorDef);
  minUpperBound = apply(profiles + errors, FUN = min, MARGIN = 1)
  maxLowerBound = apply(profiles - errors, FUN = max, MARGIN = 1)
  return(minUpperBound > maxLowerBound)
}

computeConstantSynthesis <- function(deviceSpecs, profiles) {

}

testConstantSynthesis <- function(constantSynthesisResults, errorDef = defaultErrorDef(), fitQuality = 0.8 ) {

}

#.jcall(result[[1]], "D","getError")
computeAdditiveRegulation <- function(deviceSpecs, profiles, tasks, constraints = NULL) {

  #TODO: Check parameters
  numRegulators = dim(tasks)[2] - 1;
  if(numRegulators < 1) {
    stop("The given tasks should contain at least one regulator index and one target");
  }

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
  class(processedResult) <- "gnDifferential";
  return( processedResult );
}


#Returns a matrix of integrated profiles according to the fit parameters
evaluateAdditiveRegulationResult <- function(additiveRegulationResult) {

}
