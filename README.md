This program is for creating surgical reports with a UI in Spanish.

**For Mac Users:**
Please make sure that you are running the correct .dmg file for the type of chip you have.
If you have an intel chip, download the intel version,
If you have an Apple Silicon chip, download the macos-latest version.

After installing the app to your desired folder, you may run into permission difficulties. Follow these steps to grant the application permission:

**1.** Open System Settings > Privacy & Security. Look for "Full Disk Access" in the list, and also check for a category 
       called "App Management" if your macOS version has it

**2.** In whichever of those two sections you find, click the toggle to enable it for Terminal (you may need to click the 
       lock icon and enter your password first, then use the + button to add Terminal.app if it's not already listed, 
       it's in Applications > Utilities > Terminal).

**3.** Run sudo chmod -R u+rwX /{YOUR_INSTALLATION_FOLDER}/DesktopReportBuilder.app

**4.** Enter the password that unlocks your computer

**5.** Run xattr -cr /{YOUR_INSTALLATION_FOLDER}/DesktopReportBuilder.app

Make sure that you replace {YOUR_INSTALLATION_FOLDER} to the location in which the application was installed
