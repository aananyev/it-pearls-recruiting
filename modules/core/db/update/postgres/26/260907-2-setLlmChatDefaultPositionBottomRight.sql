update HUNTTECH_USER_SETTINGS
set LLM_CHAT_BUTTON_POSITION = '{"align":"bottom-right","right":24,"bottom":24}'
where LLM_CHAT_BUTTON_POSITION is null
   or LLM_CHAT_BUTTON_POSITION = ''
   or LLM_CHAT_BUTTON_POSITION not like '%bottom-right%';
