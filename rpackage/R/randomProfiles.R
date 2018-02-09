generateRandomProfile <- function(time, scale, length) {
  # Construct the squared exponential covariance matrix
  covMatrix = array(0,c(length(time), length(time)));
  for(i in 1:length(time))  {
    covMatrix[i,i] = scale^2 + 0.00001;
    if (i < length(time)) {
      for(j in (i+1):length(time)) {
        covariance = (scale^2) * exp( (-0.5 / (length ^ 2)) * ((time[i] - time[j]) ^ 2) );
        covMatrix[i,j] = covariance
        covMatrix[j,i] = covariance
      }
    }
  }
  # Draw from the multinormal distribution using the cholesky decomposition of the cov. matrix
  cholCov = t(chol(covMatrix));
  rawProfile = cholCov %*% rnorm(length(time));
  # Transform to strictly positive values
  positiveProfile = log1p(exp(rawProfile))
  return(t(positiveProfile))
}

plotRandomProfiles <- function(n, time, scale, length, trueTime = time, trueProfile = NULL, ...) {
  profiles = array(0, c(n, length(time)));
  for(i in 1:n) {
    profiles[i,] = generateRandomProfile(time, scale, length);
  }

  ymax = max(profiles)
  if(!is.null(trueProfile)) {
    ymax = max(ymax, max(trueProfile, na.rm = TRUE))
  }

  matplot(time, t(profiles), type = "l", ylim = c(0, ymax), ...)
  if(!is.null(trueProfile)) {
    points(trueTime, trueProfile, pch = 19)
  }
}


#Eliminates profiles that are either too flat or too similar (corelation > 0.9) to the regulator profile
generateUsefulRandomProfile <- function(time, scale, length, errorDef, originalProfile) {
  numTries = 0;
  repeat { #Rejection sampling to get a non-flat, non-similar profile
    randomProfile = generateRandomProfile(time, scale, length);
    numTries = numTries + 1
    if(!testConstant(randomProfile, errorDef)) {
        if(!is.null(originalProfile)) {
          na_mask = !is.na(originalProfile)
          if(numTries > 100) {
            warning("Could not find different profile - scale: ", scale, " length: ", length, " time: ", time);
            break;
          }
          else if(cor(originalProfile[na_mask], randomProfile[1,na_mask]) < 0.9) {
            break;
          }
        }
        break;
      }
    if(numTries > 200) {
      warning("Could not create non-flat profile. - scale: ", scale, " length: ", length, " time: ", time)
      break;
    }
  }
  return(randomProfile)
}
