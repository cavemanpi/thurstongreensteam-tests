package org.thurstongreensteam.test.functional_area;

import java.util.Iterator;
import java.util.List;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import io.ddavison.selenium.AutomationTest;
import io.ddavison.selenium.Browser;
import io.ddavison.selenium.Config;
import static org.junit.Assert.*;

@Config(
	    url = "http://thurstongreensteam.org", // base url that the test launches against
	    browser = Browser.CHROME, // the browser to use.
	    hub = "" // you can specify a hub hostname / ip here.
)
public class ThurstonTests extends AutomationTest {
	
	private final int MAX_ATTEMPTS = 5;
	
	
	private String teacherUser  = "";
	private String teacherPass  = "";
	private String studentUser  = "";
	private String studentPass  = "";
	private String adminUser    = "";
	private String adminPass    = "";
	private String siteName     = "Thurston Green STEAM";
	private String adminPostfix = " ‹ Thurston Green STEAM — WordPress";
	private Config configuration = getClass().getAnnotation(Config.class);
	
	@Test
	public void videoExists() {
		waitForElement(By.cssSelector("iframe[src=\"http://www.youtube.com/embed/awj_6f744MM?feature=oembed\"]"));
		WebElement frame = driver.findElement(By.cssSelector("iframe[src=\"http://www.youtube.com/embed/awj_6f744MM?feature=oembed\"]"));
		
		assertTrue(frame != null);
	}
	
	@Test
	public void userTests() {
		String siteURL = driver.getCurrentUrl();
		
		//Test to make sure unauthenticated users cannot get to student forms.
		WebElement parentLink = waitForElement(By.partialLinkText("FIRST"));
		WebElement childLink = waitForElement(By.partialLinkText("MACRO-INVERTEBRATES"));
		new Actions(driver).moveToElement(parentLink).perform();
		new Actions(driver).moveToElement(childLink).perform();
		childLink.click();
		
		waitForWindow("Macro-Invertebrates | " + siteName);
		WebElement errorMessage = driver.findElement(By.cssSelector("#content"));
		assertTrue(errorMessage.getText().contains("be logged in to see this page."));
		List<WebElement> vfbForms = driver.findElements(By.cssSelector(".visual-form-builder"));
		assertTrue(vfbForms.size() == 0);
		
		parentLink = waitForElement(By.partialLinkText("FOURTH"));
		childLink = waitForElement(By.partialLinkText("WATER QUALITY"));
		new Actions(driver).moveToElement(parentLink).perform();
		new Actions(driver).moveToElement(childLink).perform();
		childLink.click();
		
		waitForWindow("Water Quality | " + siteName);
		errorMessage = driver.findElement(By.cssSelector("#content"));
		assertTrue(errorMessage.getText().contains("be logged in to see this page."));
		vfbForms = driver.findElements(By.cssSelector(".visual-form-builder"));
		assertTrue(vfbForms.size() == 0);
		
		
		// Test for student adding posts
		login(studentUser, studentPass);
		
		// make sure students can't edit pages.
		List<WebElement> pageMenu = driver.findElements(By.linkText("Pages"));
		assertTrue(pageMenu.size() == 0);
		
		//Make sure student can get to the posts page
		goToPosts();
		
		//Going to the new post page
		WebElement addPostLink = waitForElement(By.cssSelector("#wpbody-content a[href='" + siteURL + "wp-admin/post-new.php"));
		addPostLink.click();
		waitForWindow("Add New Post" + adminPostfix);
		
		//Creating post
		setText("[name='post_title']", "This is my selenium test post!");
		waitForElement(By.cssSelector("#content_ifr")).sendKeys("Hello World!");
		waitForElement(By.cssSelector("#sample-permalink"));
		waitForElement(By.cssSelector("input#publish[value='Submit for Review']")).click();
		//waitForText("Edit Post");

		
		//Make sure posts is not on news page yet
		driver.get(siteURL);
		waitForWindow("Thurston Green Steam | Thurston Elementary Green STEAM Project");
		waitForElement(By.partialLinkText("BLOG")).click();
		waitForWindow("Blog | Thurston Green Steam | Thurston Elementary Green STEAM Project");
		assertFalse(driver.getPageSource().contains("This is my selenium test post!"));
		
		//Log out and log back in as a teacher
		logout();
		login(teacherUser, teacherPass);
		
		// Attempt to authorize student post.
		goToPosts();
		waitForElement(By.partialLinkText("Pending")).click();
		waitForElement(By.cssSelector("a[href='edit.php?post_status=pending&post_type=post'].current"));
		waitForElement(By.partialLinkText("This is my selenium test post!")).click();
		waitForWindow("Edit Post" + adminPostfix);
		waitForElement(By.cssSelector("#sample-permalink"));
		waitForElement(By.cssSelector("input#publish")).click();
		
		//Make sure post is on the news page now
		driver.get(siteURL);
		waitForWindow("Thurston Green Steam | Thurston Elementary Green STEAM Project");
		waitForElement(By.partialLinkText("BLOG")).click();
		waitForWindow("Blog | Thurston Green Steam | Thurston Elementary Green STEAM Project");
		assertTrue(driver.getPageSource().contains("This is my selenium test post!"));
		
		//Cleanup test post
		goToPosts();
		List<WebElement> testPosts = driver.findElements(By.partialLinkText("This is my selenium test post!"));
		Iterator<WebElement> postIt = testPosts.iterator();
		while (postIt.hasNext()){
			WebElement thisElement = postIt.next();
			String link = thisElement.getAttribute("href");
			
			String postNum = link.split("\\=")[1].split("\\&")[0];
			if (postNum.length() == 0) {
				continue;
			}
			
			List<WebElement> trashLinks = driver.findElements(By.className("submitdelete"));
			Iterator<WebElement> trashIt = trashLinks.iterator();
			while (trashIt.hasNext()) {
				WebElement trashLink = trashIt.next();
				if (trashLink.getAttribute("href").contains("post=" + postNum)){
					new Actions(driver).moveToElement(trashLink).perform();
					trashLink.click();
					try {
						Thread.sleep(1500);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					break;
				}
			}
			try {
				Thread.sleep(1500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			trashLinks = driver.findElements(By.className("submitdelete"));
			trashIt = trashLinks.iterator();
		}
	}
	
	@Test
	public void donateButtonExists() {
		assertTrue(driver.getPageSource().contains("To donate to the the pond restoration, click the following"));
		waitForElement(By.cssSelector("[href='http://www.thurstonnaturecenter.org/TNCCRestorationReport.htm']"));
	}
	
	@Test
	public void formBuilderWorks() {
		login(adminUser, adminPass);
		driver.get(configuration.url() + "/wp-admin/plugins.php");
		
		//If this does not exist, there is something wrong with the form builder.
		waitForElement(By.cssSelector("#visual-form-builder-pro .row-actions .deactivate"));
		
		//log back in as a student and insert data into an existing form
		driver.get(configuration.url());
		waitForWindow(siteName);
		logout();
		login(studentUser, studentPass);
		driver.get(configuration.url());
		waitForWindow(siteName);
		
		// Go to a form.
		WebElement parentLink = waitForElement(By.partialLinkText("FIRST"));
		new Actions(driver).moveToElement(parentLink).perform();
		WebElement childLink = waitForElement(By.partialLinkText("MACRO-INVERTEBRATES"));
		new Actions(driver).moveToElement(childLink).perform();
		childLink.click();
		waitForWindow("Macro-Invertebrates | Thurston Green Steam");
		
		WebElement studentId = waitForElement(By.cssSelector(".vfb-item-text input"));
		studentId.sendKeys("12");
		
		List<WebElement> numberInputs = driver.findElements(By.cssSelector(".vfb-item-number input.digits"));
		Iterator<WebElement> numIt = numberInputs.iterator();
		
		while (numIt.hasNext()){
			WebElement numInput = numIt.next();
			double input = Math.floor(25*Math.random());
			numInput.sendKeys("" + (int)input);
		}
		
		waitForElement(By.className("vfb-submit")).click();
		waitForElement(By.id("form_success"));
		
	}

	@Test
	public void embedDocumentWorks() {
		login(adminUser, adminPass);
		driver.get(configuration.url() + "/wp-admin/plugins.php");
		
		//If this does not exist, there is something wrong with the form builder.
		waitForElement(By.cssSelector("#embed-any-document .row-actions .deactivate"));
	}

	private void goToPosts() {
		WebElement parentLink;
		WebElement childLink;
		driver.get(configuration.url() + "/wp-admin/");
		waitForWindow("Dashboard" + adminPostfix);
		parentLink = waitForElement(By.cssSelector("#menu-posts div.wp-menu-name"));
		new Actions(driver).moveToElement(parentLink).perform();
		//Sleep to give browse time to show submenu 
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		childLink = waitForElement(By.cssSelector("#menu-posts .wp-submenu a[href='edit.php']"));
		new Actions(driver).moveToElement(childLink).perform();
		childLink.click();
		waitForWindow("Posts" + adminPostfix);
	}
	
	private void login(String username, String password) {
		
		String loginURL = configuration.url() + "/wp-login.php";
		driver.get(loginURL);
		waitForElement(By.cssSelector("[name=\"log\""));
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		setText("[name='log']", username);
		setText("[name='pwd']", password);
		WebElement passwordField = driver.findElement(By.cssSelector("[name='pwd']"));
		
		assertTrue(passwordField.getAttribute("value").equals(password));
		
		passwordField.submit();
		waitForWindow("Dashboard" + adminPostfix);
	}
	
	private void logout() {
		try {
			Thread.sleep(1500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		WebElement logoutLink = waitForElement(By.partialLinkText("Log out"));
		driver.get(logoutLink.getAttribute("href"));
		try {
			waitForWindow("Thurston Green Steam › Log In");
		}
		catch(java.lang.AssertionError e) {
			waitForWindow(siteName);
		}
	}
}
