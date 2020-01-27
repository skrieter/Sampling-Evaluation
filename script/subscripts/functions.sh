#! /bin/bash
function dl_git {
	cd ../resources/systems
	DIR=$1
	if [ -d "$DIR" ]; then
		echo "Skipping ${DIR} (already exists)"
	else
		echo "Cloning $2 of $3 into $1..."
		git clone --depth 1 --branch $2 $3 $1
	fi
	cd ../../script
}

function dl_wget {
	cd ../resources/systems
	DIR=$1
	if [ -d "$DIR" ]; then
		echo "Skipping ${DIR} (already exists)"
	else
		echo "Downloading ${DIR}..."
		wget -N -nv $3/$2
		echo "Extracting ${DIR}..."
		tar -xzf $2
	fi
	cd ../../script 
}

function run_kclause {
	cd subscripts
	sh kclause.sh $1 $2
	cd ..
}
