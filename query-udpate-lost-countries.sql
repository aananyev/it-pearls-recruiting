update itpearls_job_candidate 
set current_company_id = 
	(select b.id 
	from itpearls_company b 
	where b.comany_name like 'Безработный%') 
where full_name in 
	(select e.full_name 
	from itpearls_job_candidate e, itpearls_company f 
	where not exists 
		(select * 
		from itpearls_company g 
		where g.id = e.current_company_id ) 
	group by e.full_name);
