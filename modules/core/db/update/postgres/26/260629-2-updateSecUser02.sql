-- Migrate legacy admin photo (IMAGE_ID) to officialPhoto
update SEC_USER
set OFFICIAL_PHOTO_ID = IMAGE_ID
where IMAGE_ID is not null;

-- Migrate personal photo from UserSettings to userAvatar
update SEC_USER u
set USER_AVATAR_ID = us.IMAGE_ID
from ITPEARLS_USER_SETTINGS us
where us.USER_ID = u.ID
  and us.IMAGE_ID is not null;

-- Default userAvatar from officialPhoto when user has no personal photo
update SEC_USER
set USER_AVATAR_ID = OFFICIAL_PHOTO_ID
where USER_AVATAR_ID is null
  and OFFICIAL_PHOTO_ID is not null;
