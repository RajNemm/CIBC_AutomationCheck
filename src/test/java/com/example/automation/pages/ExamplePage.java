package com.example.automation.pages;

import com.example.automation.config.Config;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class ExamplePage extends BasePage {
    private static final By HEADING = By.tagName("h1");

    public ExamplePage(WebDriver driver) {
        super(driver);
    }

    public ExamplePage open() {
        driver.get(Config.baseUrl());
        return this;
    }

    public String headingText() {
        return textOf(HEADING);
    }
}
