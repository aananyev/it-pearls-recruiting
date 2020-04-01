update itpearls_job_candidate 
set CITY_OF_RESIDENCE_ID = 
	(select b.id 
	from itpearls_city b 
	where b.CITY_RU_NAME like 'Москва') 
where full_name in 
	(select e.full_name 
	from itpearls_job_candidate e, itpearls_city f 
	where not exists 
		(select * 
		from itpearls_city g 
		where g.id = e.city_of_residence_id ) 
group by e.full_name);
