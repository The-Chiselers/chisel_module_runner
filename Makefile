MAKEFLAGS += --silent

# Define SBT variable
SBT = sbt

# Add phony targets
.PHONY: clean update

update:
	@echo Updating...
	sbt clean update

# Start with a fresh directory
clean:
	@echo Cleaning...
	rm -rf generated target *anno.json ./*.rpt doc/*.rpt syn/*.rpt syn.log out test_run_dir target
	rm -rf project/project project/target
	# filter all files with bad extensions
	find . -type f -name "*.aux" -delete
	find . -type f -name "*.toc" -delete
	find . -type f -name "*.out" -delete
	find . -type f -name "*.log" -delete
	find . -type f -name "*.fdb_latexmk" -delete
	find . -type f -name "*.fls" -delete
	find . -type f -name "*.synctex.gz" -delete
	find . -type f -name "*.pdf" -delete
