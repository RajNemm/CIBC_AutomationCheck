package com.example.automation.tests;

import com.example.automation.pages.ExamplePage;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ExamplePageTest extends BaseTest {
    @Test(groups = "smoke")
    public void examplePageDisplaysExpectedHeading() {
        ExamplePage page = new ExamplePage(driver).open();

        Assert.assertEquals(page.headingText(), "Example Domain");
    }
}
