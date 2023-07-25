[![Codacy Badge](https://app.codacy.com/project/badge/Grade/d9671224fbb9449eb4ed373289c4059f)](https://app.codacy.com/gh/kaygenzo/BugReporter/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade) [![Maven Central](https://img.shields.io/maven-central/v/com.github.kaygenzo/bugreporter.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.kaygenzo%22%20AND%20a:%22bugreporter%22)

# BugReporter
Bug reporting library to let the user report an issue of a bug. 
After clicking on the send button, the user is redirected to a content share provider provider with all information

## Installation

```groovy
implementation "io.github.kaygenzo:androidtable:$tablemultiscrollVersion"
```

## Usage

### Create a bug reporter instance

```kotlin
val reporter: BugReporter = BugReporter.Builder()
    .setCompressionQuality(75)
    .setImagePreviewScale(0.3f)
    .setFields(null)
    .setEmail("developer@telen.fr")
    .setReportFloatingImage(R.drawable.images)
    .observeResult(this)
    .setDebug(false)
    .build(this)
```
All fields of the builder can be changed regarding your needs, but default values would be okay for most of the use cases.

Fields can be found in enum FieldType and possible value are
```kotlin
enum class FieldType {
    DATE_TIME,
    DATE_TIME_MILLIS,
    MANUFACTURER,
    BRAND,
    MODEL,
    APP_VERSION,
    ANDROID_VERSION,
    LOCALE,
    SCREEN_DENSITY,
    SCREEN_RESOLUTION,
    ORIENTATION,
    BATTERY_STATUS,
    BT_STATUS,
    WIFI_STATUS,
    NETWORK_STATUS
}
```
Null value means all values. 

The reporting floating image will be the image shown in the floating button when one of the the reporting methods will be set as floating button

The observe result method will let you get the intent with all the information filled and ready to be used. Feel free to change it to match your personal needs.

### Use the bug reporter in your app

```kotlin
reporter.disable()
```
Disable the bug reporter, no reports will be generated

```kotlin
reporter.startReport(this)
```
Start the bug reporting flow. It will start an activity with information and screenshot, which will be shown to the final user and will let him personalize the report.

```kotlin
reporter.setReportMethods(listOf(ReportMethod.SHAKE, ReportMethod.FLOATING_BUTTON))
reporter.restart()
```
Will set the methods used to trigger the bug report. An empty list means the trigger will be manual (see startReport above)

```kotlin
reporter.release()
```
To clean properly all dependencies of the bug reporter.

```kotlin
reporter.askOverlayPermission(activity, REQUEST_CODE_PERMISSION)
```
Launch the native settings screen to ask the permission to display the floating button on top of any applications.

## Contributing
Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

Please make sure to update tests as appropriate.

## License
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.