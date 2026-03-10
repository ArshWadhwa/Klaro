package org.example.group;

import java.awt.*;
import java.util.List;


public class OpenRouterResponse {

    private List<Choice> choices;

    public OpenRouterResponse(List<Choice> choices) {
        this.choices = choices;
    }

    public List<Choice> getChoices() {
        return choices;
    }

//    public void setChoices(List<Choice> choices) {
//        this.choices = choices;
//    }
}
