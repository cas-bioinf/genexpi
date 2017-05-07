generateRandomProfile <- function(time, variance, length) {
  covMatrix = array(0,c(length(time), length(time)));
  for(i in 1:length(time))  {
    covMatrix[i,i] = variance + 0.00001;
    if (i < length(time)) {
      for(j in (i+1):length(time)) {
        #TOTO je asi spatne
        covariance = variance * ((0.5) / exp(((time[i] - time[j])/length) ^ 2));
        covMatrix[i,j] = covariance
        covMatrix[j,i] = covariance
      }
    }
  }
  cholCov = chol(covMatrix);
  rawProfile = t(cholCov %*% rnorm(length(time)));
  return(log(1 + exp(rawProfile)))
}

plotRandomProfiles <- function(n, time, variance, length) {
  profiles = array(0, c(n, length(time)));
  for(i in 1:n) {
    profiles[i,] = generateRandomProfile(time, variance, length);
  }
  matplot(time, t(profiles), type = "l")
}

testRandomRegulator <- function(deviceSpecs, rounds, profiles, time, variance, length, errorDef = defaultErrorDef()) {
  numProfiles = dim(profiles)[1];
  numTime = dim(profiles)[2];
  profilesWithRegulator = array(0, c(numProfiles + 1, numTime));
  profilesWithRegulator[1:numProfiles,] = profiles;

  fitQualities = array(0, c(rounds, numProfiles));

  tasks = array(0, c(numProfiles,2));
  tasks[,1] = numProfiles + 1 #The regulator
  tasks[,2] = 1:numProfiles; #The targets

  constraints = NULL #TODO positive only

  for(round in 1:rounds) {
    randomProfile = generateRandomProfile(time, variance, length);
    if(testConstant(randomProfile, errorDef)) {
      fitQualitites[r,] = NA;
    }
    else {
      profilesWithRegulator[profilesDim[1] + 1,] = randomProfile;
      computationResult = computeAdditiveRegulation(deviceSpecs, profilesWithRegulaor, tasks, constraints = constraints);
      fittedProfiles = evaluateAdditiveRegulationResult(computationResult);
      numFits = 0;
      for(i in 1:numProfiles) {
        fitQualities[r,i] = profileFit(profiles[i,], fittedProfiles[i,], errorDef)
      }

    }
  }
  return(fitQualities);
}
