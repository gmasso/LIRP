all: LocationInventoryRouting.pdf

LocationInventoryRouting.pdf: *.tex figures/* *.bib
	pdflatex LocationInventoryRouting.tex 
	bibtex LocationInventoryRouting.aux
	pdflatex LocationInventoryRouting.tex 
	pdflatex LocationInventoryRouting.tex 

clean:
	rm *.pdf *.aux *.bbl
