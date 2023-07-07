package com.company.itpearls.web.screens.openpositioncomment;

import com.company.itpearls.core.StarsAndOtherService;
import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.OpenPositionComment;
import com.haulmont.cuba.security.entity.User;

import javax.inject.Inject;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@UiController("itpearls_OpenPositionComment.edit")
@UiDescriptor("open-position-comment-edit.xml")
@EditedEntityContainer("openPositionCommentDc")
@LoadDataBeforeShow
public class OpenPositionCommentEdit extends StandardEditor<OpenPositionComment> {
    @Inject
    private StarsAndOtherService starsAndOtherService;
    @Inject
    private LookupField ratingField;
    @Inject
    private Label<String> ratingLabel;
    @Inject
    private LookupPickerField<OpenPosition> openPositionField;
    @Inject
    private PickerField<User> userField;
    @Inject
    private DateField<Date> dateCommentField;

    @Subscribe
    public void onInit(InitEvent event) {
        setRatingField();
    }

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if(PersistenceHelper.isNew(getEditedEntity())) {
            dateCommentField.setValue(new Date());
        }
    }

    public void setOpenPositionField(OpenPosition openPosition) {
        if (openPosition != null) {
            openPositionField.setValue(openPosition);
        }
    }

    public void setUserField(User user) {
        if (user != null) {
            userField.setValue(user);
        }
    }

    private void setRatingField() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put(starsAndOtherService.setStars(1) + " Полный негатив", 0);
        map.put(starsAndOtherService.setStars(2) + " Сомнительно", 1);
        map.put(starsAndOtherService.setStars(3) + " Нейтрально", 2);
        map.put(starsAndOtherService.setStars(4) + " Положительно", 3);
        map.put(starsAndOtherService.setStars(5) + " Отлично!", 4);
        ratingField.setOptionsMap(map);

        ratingField.addValueChangeListener(e -> {
            ratingLabel.setValue(String.valueOf(ratingField.getValue() != null ?
                    ((int) ratingField.getValue()) + 1 :
                    0));
            String rating_color = "";

            if (ratingField.getValue() != null) {
                switch ((int) ratingField.getValue()) {
                    case 0:
                        rating_color = "red";
                        break;
                    case 1:
                        rating_color = "orange";
                        break;
                    case 2:
                        rating_color = "yellow";
                        break;
                    case 3:
                        rating_color = "green";
                        break;
                    case 4:
                        rating_color = "blue";
                        break;
                    default:
                        rating_color = "red";
                        break;
                }
            }

            String rating_style = "rating_" + rating_color + "_"
                    + String.valueOf(ratingField.getValue() != null ? ((int) ratingField.getValue()) + 1 : 1);

//            ratingField.setStyleName(rating_style);
            ratingLabel.setStyleName(rating_style);
        });
    }
}