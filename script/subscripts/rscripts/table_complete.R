data.tab = data.agg

data.tab$ModelName = gsub("_", "\\\\_", data.tab$ModelName)
data.tab$Size = floor(data.tab$Size)
data.tab$Time = floor(data.tab$Time)

nameFactors = factor(data.tab$ModelName, levels=unique(data.tab$ModelName))
data.list = split(data.tab, nameFactors)

file.output = paste(dir.output, "tab_data.tex", sep = "/")
if (file.exists(file.output)) {
	file.remove(file.output)
}

out = function(line) {
	cat(line)
	cat(line, file = file.output, append = TRUE)
}

# Header
out("Model & Property")
for (a in data.algorithms) {
	out(" & ")
	out(gsub("_", "\\\\_", a))
}
out(" \\\\\n")
out("\\midrule\n")

# Rows
for (d in data.list) {
  out(d[1, ModelName])
  out(" & Time")
  minValue = min(d[, Time])
  for (a in data.algorithms) {
  	out(" & ")
  	value = d[Algorithm == a, Time]
  	if (length(value) == 1) {
  	  	if (value == minValue) {
	  	  	out("\\textbf{\\textit{")
	 	 	out(as.vector(value))
			out("}}") 		
	  	} else {
	  	  	out(as.vector(value))
		}
	} else if (length(value) == 0) {
		out(" -- ")	
	} else if (length(Value) > 1) {
		out(" ??? ")
	}
  }
  out(" \\\\\n")

  out(" & Size")
  minValue = min(d[, Size])
  for (a in data.algorithms) {
  	out(" & ")
  	value = d[Algorithm == a, Size]
  	if (length(value) == 1) {
  	  	if (value == minValue) {
	  	  	out("\\textbf{\\textit{")
	 	 	out(as.vector(value))
			out("}}") 		
	  	} else {
	  	  	out(as.vector(value))
		}
	} else if (length(value) == 0) {
		out(" -- ")	
	} else if (length(Value) > 1) {
		out(" ??? ")
	}
  }
  out(" \\\\\n")

  maxSize = max(d[, Size])
  maxTime = max(d[, Time])
  maxValue = maxSize * maxTime
  out(" & Ratio")
  minValue = floor(10000 * min(d[, Time * Size / maxValue])) / 100
  for (a in data.algorithms) {
  	out(" & ")
  	value = floor(10000 * d[Algorithm == a, Time * Size / maxValue]) / 100
  	if (length(value) == 1) {
  	  	if (value == minValue) {
	  	  	out("\\textbf{\\textit{")
	 	 	out(as.vector(value))
			out("}}") 		
	  	} else {
	  	  	out(as.vector(value))
		}
	} else if (length(value) == 0) {
		out(" -- ")	
	} else if (length(Value) > 1) {
		out(" ??? ")
	}
  }
  out(" \\\\\n")
  out("\\midrule\n")
}