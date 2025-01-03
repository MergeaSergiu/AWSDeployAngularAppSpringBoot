# Spring Boot API for Deploying Angular Applications


Overview
This is a Spring Boot API that allows users to deploy their Angular applications. The users can upload a ZIP file containing the Angular app, and the application will be automatically extracted, built, and deployed to an AWS S3 bucket.

# Features

###
Authentication and Authorization: Users need to authenticate to access the resources and the authorization is handled using JWT
###
Upload Angular Application: Users can upload a ZIP file containing their Angular app.
###
Automatic Build: The API will unzip the application and handle the build process of the Angular app.
###
AWS Integration: The app uses AWS services to store the built files in an S3 bucket.
###
CloudFront: Once the app is deployed to S3, a CloudFront distribution is created for easy access to the app.

# Functionalities

Sign Up
###
Log In
###
Refresh Access Token
####
Create bucket
####
Delete bucket
###
Retrieve list of buckets
###
Retrieve of CloudFront distributions
###
Upload ZIP archive
###
Send emails when bucket is created and application is deployed
####
Update Application status (enable/disable)
###
Delete distribution

# Technologies

Java 17, Spring, Postgre SQL, AWS SES, AWS S3, AWS CodeBuild, AWS CloudFront

# AWSDeployAngularAppSpringBoot
