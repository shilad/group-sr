This file contains all the possible combinations of group aggregate ratings.

The first part of the filename (all / general / specific) denotes which subset of concept pairs appear in the file.

 * All contain all concept pairs (a maximum of 200 pairs, if the group rated everything).
 * General contain general concept-pairs (a maximum of 50 questions).
 * Specific contains domain-specific concept-pairs (a maximum of 50 questions for history / psych / bio, but 150 for other groups because all 3 fields are combined).


The second part of the filename (history / biology / psych / mturk / scholar / scholar-in / scholar-all) represents the group:

 * history / biology / psychology: Scholars with expertise in those fields.
 * mturk: Turkers
 * scholar-in: Scholars who are experts in the concept pair. Only makes sense for specific concept-pairs.  
 * scholar: Scholars. Excludes scholar-in for specific concept pairs.
 * scholar-all: Both scholars and scholars-in.
 
 Statistics follow:
 
```
	all-history.txt n=199 (sum=1082 min=1, mean=5.33, median=5.00, max=13)
	all-biology.txt n=200 (sum=1297 min=1, mean=6.36, median=6.00, max=18)
	all-psychology.txt n=183 (sum=885 min=1, mean=4.73, median=5.00, max=10)
	all-mturk.txt n=200 (sum=2835 min=6, mean=13.90, median=14.00, max=39)
	all-scholar.txt n=200 (sum=2288 min=3, mean=11.22, median=11.00, max=30)
	all-scholar-all.txt n=200 (sum=5552 min=12, mean=27.22, median=28.00, max=71)
	general-history.txt n=49 (sum=175 min=1, mean=3.57, median=3.00, max=8)
	specific-history.txt n=50 (sum=434 min=5, mean=8.68, median=8.00, max=13)
	general-biology.txt n=50 (sum=205 min=1, mean=4.10, median=4.00, max=8)
	specific-biology.txt n=50 (sum=512 min=8, mean=10.24, median=10.00, max=13)
	general-psychology.txt n=48 (sum=142 min=1, mean=2.96, median=3.00, max=7)
	specific-psychology.txt n=50 (sum=349 min=5, mean=6.98, median=7.00, max=10)
	general-mturk.txt n=50 (sum=461 min=6, mean=9.22, median=9.00, max=13)
	specific-mturk.txt n=150 (sum=2218 min=7, mean=14.79, median=15.00, max=20)
	general-scholar.txt n=50 (sum=887 min=12, mean=17.74, median=18.00, max=27)
	specific-scholar.txt n=150 (sum=3086 min=10, mean=20.57, median=21.00, max=28)
	specific-scholar-in.txt n=150 (sum=1295 min=5, mean=8.63, median=9.00, max=13)
	general-scholar-all.txt n=50 (sum=887 min=12, mean=17.74, median=18.00, max=27)
	specific-scholar-all.txt n=150 (sum=4381 min=20, mean=29.21, median=29.00, max=40)
```