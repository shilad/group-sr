PluraSR200
=============

This directory contains the pluraSR200 dataset. 
If you use this dataset, please cite: 
Sen et al., "Turkers, Scholars, “Arafat” and “Peace”: Cultural Communities and Algorithmic Gold Standards", *CSCW 2015*. [[pdf] | http://www-users.cs.umn.edu/~bhecht/publications/goldstandards_CSCW2015.pdf]. 
The paper also details the survey methodology used for the dataset.

The "general" datasets contain general concept-pairs from WordSim 353 (50 questions). 
Responses from virtual workers on Amazon's Mechanical Turk ("turkers") and scholars are separated into different files.
Each file contains three four-delimited fields: concept 1, concept 2, the mean response, and the number of subjects.
The responses are on a one to five point scale.

The information below lists the filenames, along with statistics describing the number of responses for each concept pair.
 
```
	general-turker.txt n=50 (sum=461 min=6, mean=9.22, median=9.00, max=13)
	general-scholar.txt n=50 (sum=887 min=12, mean=17.74, median=18.00, max=27)
```

The "specific" dataset contains responses for concept-pairs from the fields of history, psychology, and biology. 
There are 50 pairs in each field, for a total of 150 pairs. The files below contain responses (in the same format 
as above) from turkers, scholars who are experts in the field ("scholar-expert") and other scholars.
 
```
	specific-turker.txt n=150 (sum=2218 min=7, mean=14.79, median=15.00, max=20)
	specific-scholar.txt n=150 (sum=3086 min=10, mean=20.57, median=21.00, max=28)
	specific-scholar-expert.txt n=150 (sum=1295 min=5, mean=8.63, median=9.00, max=13)
```

In addition, we have broken out the scholar-expert responses by subject:

```
	specific-history.txt n=50 (sum=434 min=5, mean=8.68, median=8.00, max=13)
	specific-biology.txt n=50 (sum=512 min=8, mean=10.24, median=10.00, max=13)
	specific-psychology.txt n=50 (sum=349 min=5, mean=6.98, median=7.00, max=10)
```