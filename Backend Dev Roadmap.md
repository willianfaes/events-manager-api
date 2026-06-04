* Code First: Build the core logic. (Entities, Controllers, Mappers, Pagination, Soft Deletes).
	Pagination & Sorting:
	Difficulty: Easy
	Time: 1–2 days. Spring Data JPA literally has a built-in Pageable object. You just pass it into your repository, and Spring writes the complex SQL limit/offset queries for you.

	Soft Deletes:
	Difficulty: Easy
	Time: 1 day. In modern Hibernate (which Spring Boot uses), this is often just adding an @SQLRestriction and @SQLDelete annotation to your entity.

	User Profiles (1-to-1 Relationship):
	Difficulty: Easy to Medium
	Time: 2–3 days. This is just practicing JPA relationships (@OneToOne). The logic is straightforward, but setting up the Mappers for it takes a little focus.

	Advanced Filtering (Search):
	Difficulty: Medium
	Time: 3–5 days. This is the hardest of the bunch. You will have to learn how to write custom @Query strings or use JPA Specifications to handle situations where a user searches by date, but leaves the category blank.

* Global Exception Handling: Make sure your API returns clean, professional errors instead of massive Java stack traces.

* Testing (JUnit/Mockito): Write tests to prove your code works. (remember CI/CD server will run my tests to then send it to Test server? yeah... my tests must be good!)

* Documentation (Swagger): Add Swagger so a frontend developer knows how to use your API.

* Containerization (Docker): Wrap your app and your MySQL database in Docker so it runs anywhere.

* The Cloud (AWS + CI/CD): Deploy that Docker container to AWS and automate the deployment with GitHub Actions.

Obs: 
	To deploy your API, you only need to learn three specific AWS services:
	RDS (Relational Database Service): To host your MySQL database in the cloud.
	EC2 (Elastic Compute Cloud): To run your Spring Boot .jar file.
	S3 (Simple Storage Service): Optional, but needed if you want users to upload profile pictures.

* Learn linux basic terminal commands. Maybe something else? 99% of backend servers run on linux! When production server crash, a tech lead expects me to SSH into the Linux server and look at the logs to find the error. If I look like a deer in headlights when the terminal opens... it's a red flag.

* Java code style convention: What tools devs use to check and enforce the Code Convention they are following? Remember: Make sure the final code fully respects the Oracle Java Convention (tho there are other, stick to this one). A professional portfolio follow professional standards!

* Do devs still relly on manual git commands on terminal at the job? What is the current enterprise behaviour regarding DOCKER and GIT terminal code? Rely on manual inputs, or AI? FIND OUT!

* 🌥️ How to Include AWS Without the Exam
If you want to show employers you care about the cloud, spend just one weekend deployment-testing. Do not take an exam. Instead, open a free AWS account, take your Dockerized Spring Boot API, and manually deploy it to AWS Elastic Beanstalk or an AWS EC2 instance.
Adding the line "Successfully deployed a containerized Spring Boot API onto AWS" to your resume will grab a Turin tech recruiter's attention much faster than a theoretical certificate—and it will take you a fraction of the time to achieve.

* 🔨 The "Show, Don't Tell" Strategy for Turin Engineers
Since your goal is to talk directly to engineers and show them you are not just a "regular student" who only knows theory, your resume and GitHub need to do the talking for you. Italian university CS programs are notoriously heavy on theoretical mathematics and very light on actual coding. !!!!!----** You can exploit this gap.----!!!!!
When an engineer opens your GitHub in August, here is what will make them say "Wow, this person is ahead of local graduates":
A Spotless Git History: Since you are comfortable with Git Flow, ensure your portfolio repository doesn't have a single giant commit called "initial commit." Show a clean history of feature branches, descriptive commit messages (e.g., feat: implement user authentication endpoint), and pull requests. Engineers look at commit history to see how you think.
The "Turin-Specific" Project Idea: Instead of building a generic Todo app, build a REST API that solves a relatable problem. For example: A backend for a "Torino Bike/Scooter Sharing aggregator" or a "University Exam Scheduler."
An Impressive README: Write your project documentation in flawless English. Include a diagram of your database architecture, a list of your API endpoints, and a quick "How to Run" section using Docker Compose.

* Practicing JDBC (Java Database Connectivity) or building simple CLI (Command Line Interface) applications:
Write a pure Java program that connects to your Dockerized PostgreSQL database.
Practice writing raw SQL queries (INSERT, SELECT, UPDATE, DELETE) directly inside your Java code.
Handle database connection errors properly using Java's try-catch blocks.
This will give you a flawless transition into July, because Spring Boot handles all of this database connection work automatically behind the scenes. Understanding how it works manually first will make you a much stronger developer.
