import org.scalatest.BeforeAndAfterAll
import org.scalatestplus.play.{OneAppPerSuite, PlaySpec}
import play.api.Logger
import play.api.test.FakeApplication
import play.api.test.Helpers._

class FileTests extends PlaySpec with OneAppPerSuite with BeforeAndAfterAll {
  implicit val context = play.api.libs.concurrent.Execution.Implicits.defaultContext
  lazy val logger = Logger(classOf[FileTests])

  val gitlabAPI = GitlabHelper.gitlabAPI
  val projectName = GitlabHelper.projectName

  var projectId = -1

  val fileName = "test_file"
  val fileContent = "test_content"
  val fileCommitMessage = "test_commit_message"
  var targetBranch = "master"

  override def beforeAll(): Unit = {
    running(FakeApplication()) {
      GitlabHelper.createTestSSHKey
      projectId = GitlabHelper.createTestProject
      logger.debug("Starting File Tests")
    }
  }

  override def afterAll() {
    running(FakeApplication()) {
      try {
        val response = await(gitlabAPI.deleteFile(projectId, fileName, targetBranch, fileCommitMessage))
        // Returns 400 ("You can only edit text files") if it tries to delete a non-existent file?
        GitlabHelper.checkDeleteAfterTest(response, FILE)
      } catch {
        case e: UnsupportedOperationException => logger.error(e.toString)
      }
      GitlabHelper.deleteTestSSHKey()
      GitlabHelper.deleteTestProject()
      logger.debug("End of File Tests")
      Thread.sleep(1000L)
    }
  }

  "GitlabAPI must manage repository files" should {

    "create file" in {
      val response = await(gitlabAPI.createFile(projectId, fileName, targetBranch, fileContent, fileCommitMessage))
      response.status must be(201)
      (response.json \ "file_path").as[String] must be(fileName)
    }

    "get file" in {
      await(gitlabAPI.getFile(projectId, fileName, targetBranch)).status must be(200)
    }

    "update file" in {
      await(gitlabAPI.updateFile(projectId, fileName, targetBranch, "update", fileCommitMessage)).status must be(200)
      val response = await(gitlabAPI.getFile(projectId, fileName, targetBranch))
      response.status must be (200)
      // TODO Does it encode by default instead of text? Gitlab encoding looks weird?
      // BaseEncoding.base64().decode((response.json \ "content").as[String]) must be("update")
    }

    "delete file" in {
      val response = await(gitlabAPI.deleteFile(projectId, fileName, targetBranch, fileCommitMessage))
      response.status must be(200)
      response.json must not be null
    }

  }
}
