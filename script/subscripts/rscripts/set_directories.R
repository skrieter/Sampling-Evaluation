dir.data = "../../../gen/results"

dir.data.sub <- min(list.dirs(path = dir.data, recursive = FALSE))
dir.output <- paste(dir.data.sub, "plots", sep = "/")
print(dir.data.sub)

if (!dir.exists(dir.output)) {
	dir.create(dir.output)
}