#!/bin/bash
rm ./out/*
latexmk -pdf -output-directory=out paper.tex
