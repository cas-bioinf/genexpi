defaultErrorDef <- function() {
  return(list(absolute = 0, relative = 0.2, minimal = 0))
}

errorMargin <- function(x, errorDef = defaultErrorDef()) {
  errors = x * errorDef$relative + errorDef$absolute;
  errors[errors < errorDef$minimal] = errorDef$minimal;
  return(errors)
}

fitQuality <- function(targetProfile, candidateProfile, errorDef = defaultErrorDef()) {
  if(length(targetProfile) != length(candidateProfile)) {
    error("Profiles must have the same length")
  }
  matches = abs(targetProfile - candidateProfile) < errorMargin(targetProfile, errorDef);
  return(mean(matches));
}
