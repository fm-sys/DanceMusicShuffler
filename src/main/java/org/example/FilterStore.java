package org.example;

public class FilterStore extends AbstractStore<String> {
    @Override
    protected String defaultState() {
        return "";
    }

    @Override
    public void setState(String newState) {
        if (newState == null || get().equals(newState)) {
            return;
        }
        super.setState(newState);
    }

    public String normalized() {
        return normalized(get());
    }

    private String normalized(String value) {
        return value.toLowerCase().trim();
    }
}

