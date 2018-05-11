
[![Build Status](https://travis-ci.org/Miha-x64/reactive-properties.svg?branch=master)](https://travis-ci.org/Miha-x64/reactive-properties)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/89813e3ee28441b3937a76f09e906aef)](https://www.codacy.com/app/Miha-x64/reactive-properties?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=Miha-x64/reactive-properties&amp;utm_campaign=Badge_Grade)
![Almost Lock-free](https://img.shields.io/badge/%E2%9A%9B-Almost%20Lock--free-3399aa.svg) 
![Extremely lightweight](https://img.shields.io/badge/🦋-Extremely%20Lightweight-7799cc.svg)

[![codecov](https://codecov.io/gh/Miha-x64/reactive-properties/branch/master/graph/badge.svg)](https://codecov.io/gh/Miha-x64/reactive-properties) in module `:properties`, excluding inline functions

## Adding to a project

[![Download](https://api.bintray.com/packages/miha-x64/maven/net.aquadc.properties%3Aproperties/images/download.svg)](https://bintray.com/miha-x64/maven/net.aquadc.properties%3Aproperties/_latestVersion) Reactive Properties

[![Download](https://api.bintray.com/packages/miha-x64/maven/net.aquadc.properties%3Aandroid-bindings/images/download.svg)](https://bintray.com/miha-x64/maven/net.aquadc.properties%3Aandroid-bindings/_latestVersion) Android Bindings

```
repositories {
    ...
    maven { url 'https://dl.bintray.com/miha-x64/maven' }
}
```

```
dependencies {
    // JVM
    compile 'net.aquadc.properties:properties:0.0.2'

    // Android + Gradle 4
    implementation 'net.aquadc.properties:properties:0.0.2'
    implementation 'net.aquadc.properties:android-bindings:0.0.2'

    // also requires Kotlin-stdlib to be added into a project
}
```

# reactive-properties

Properties (subjects) inspired by JavaFX MVVM-like approach.
* Lightweight
* Contains both unsynchronized and concurrent (lock-free) implementations
* MVVM / data-binding for Android and JavaFX
* Sweeter with [Anko-layouts](https://github.com/Kotlin/anko#anko-layouts-wiki) 
  and [TornadoFX](https://github.com/edvin/tornadofx)
* Depends only on Kotlin-stdlib
* [Presentation](https://speakerdeck.com/gdg_rnd/mikhail-goriunov-advanced-kotlin-patterns-on-android-properties)
   — problem statement, explanations

## Alternatives

* [agrosner/KBinding](https://github.com/agrosner/KBinding) (MIT) — similar to this,
  Observable-based, Android-only, depends on coroutines
* [BennyWang/KBinding](https://github.com/BennyWang/KBinding) (no license) —
  Android-only, based on annotation processing, depends on RXJava 1.3,
* [LewisRhine/AnkoDataBindingTest](https://github.com/LewisRhine/AnkoDataBindingTest)
   (no license) 
   — theoretical solution from [Data binding in Anko](https://medium.com/lewisrhine/data-binding-in-anko-77cd11408cf9)
   article, Android-only, depends on Anko and AppCompat

## Sample

```kt
val prop: MutableProperty&lt;Int&gt; = propertyOf(1)
val mapped: Property&lt;Int&gt; = prop.map { 10 * it }
assertEquals(10, mapped.value)

prop.value = 5
assertEquals(50, mapped.value)


val tru = propertyOf(true)
val fals = !tru // operator overloading
assertEquals(false, fals.value)
```

## Sample usage in GUI application

Anko layout for Android:

```kt
verticalLayout {
    padding = dip(16)

    editText {
        hint = "Email"
        bindTextBidirectionally(vm.emailProp)
        bindErrorMessageTo(vm.emailValidProp.map { if (it) null else "E-mail is invalid" })
    }

    editText {
        hint = "Name"
        bindTextBidirectionally(vm.nameProp)
    }

    editText {
        hint = "Surname"
        bindTextBidirectionally(vm.surnameProp)
    }

    button {
        bindEnabledTo(vm.buttonEnabledProp)
        bindTextTo(vm.buttonTextProp)
        setWhenClicked(vm.buttonClickedProp)
        // ^ set flag on action
    }

}
```

JavaFx layout (using JFoenix):

```kt
children.add(JFXTextField().apply {
    promptText = "Email"
    textProperty().bindBidirectionally(vm.emailProp)
})

children.add(Label().apply {
    text = "E-mail is invalid"
    bindVisibilityHardlyTo(!vm.emailValidProp)
})

children.add(JFXTextField().apply {
    promptText = "Name"
    textProperty().bindBidirectionally(vm.nameProp)
})

children.add(JFXTextField().apply {
    promptText = "Surname"
    textProperty().bindBidirectionally(vm.surnameProp)
})

children.add(JFXButton("Press me, hey, you!").apply {
    disableProperty().bindTo(!vm.buttonEnabledProp)
    textProperty().bindTo(vm.buttonTextProp)
    setOnAction { vm.buttonClickedProp.set() }
})
```

Common ViewModel:

```kt
val emailProp = propertyOf(userProp.value.email)
val nameProp = propertyOf(userProp.value.name)
val surnameProp = propertyOf(userProp.value.surname)
val buttonClickedProp = propertyOf(false)

val emailValidProp = propertyOf(false)
val buttonEnabledProp = propertyOf(false)
val buttonTextProp = propertyOf("")

private val editedUser = OnScreenUser(
        emailProp = emailProp,
        nameProp = nameProp,
        surnameProp = surnameProp
)

init {
    // check equals() every time User on screen on in memory gets changed
    val usersEqualProp = listOf(userProp, emailProp, nameProp, surnameProp)
            .mapValueList { _ -> userProp.value.equals(editedUser) }

    emailValidProp.bindTo(emailProp.map { it.contains("@") })
    buttonEnabledProp.bindTo(usersEqualProp.mapWith(emailValidProp) { equal, valid -> !equal && valid })
    buttonTextProp.bindTo(usersEqualProp.map { if (it) "Nothing changed" else "Save changes" })

    buttonClickedProp.clearEachAnd { userProp.value = editedUser.snapshot() }
    // ^ reset flag and perform action — store User being edited into memory
}
```

## ProGuard rules for Android
(assume you depend on `:properties` and `:android-bindings`)

```
# using annotations with 'provided' scope
-dontwarn android.support.annotation.**

# bindings to design lib whish has 'provided' scope
-dontwarn android.support.design.widget.**

# safely checking for JavaFX which is not accessible on Android
-dontwarn javafx.application.Platform

# keep volatile field names for AtomicFieldUpdater
-keepclassmembernames class net.aquadc.properties.internal.** {
  volatile <fields>;
}
-keepclassmembernames class net.aquadc.properties.android.pref.SharedPreferenceProperty {
  volatile <fields>;
}
```
