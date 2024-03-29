package views.search

import Exts.unaccent
import javafx.event.EventHandler
import javafx.scene.control.ProgressIndicator
import javafx.scene.input.KeyCode
import javafx.scene.paint.Color
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import models.Torrent
import tornadofx.*
import views.servers.ServersStatusView
import views.servers.Verification

class SearchView : View("Torrent Search Engine"), Verification {


    private val controller: SearchController by inject()
    private lateinit var progressIndicator: ProgressIndicator
    private var serversStatusModal: ServersStatusView = ServersStatusView(this)
    private var modal: Stage? = null


    override val root =
        // TODO: Convert to BorderPane

        vbox {

            menubar {
                menu("Tools") {
                    item("Check servers").action {
                        modal = serversStatusModal.openModal(
                            stageStyle = StageStyle.UTILITY,
                            escapeClosesWindow = true,
                            modality = Modality.NONE,
                            owner = null,
                            block = false
                        )
                    }
                }
                menu("Help") {
                    item("About")
                    separator()
                    item("How to use")
                }
            }



            /** Search input */
            vbox {
                paddingTop = 10
                paddingLeft = 10
                text("Search") {
                    spacing = 5.0
                }
                hbox {
                    fitToParentWidth()
                    textfield {
                        minWidth = 500.0
                        spacing = 20.0
                        onKeyReleased = EventHandler {
                            if (it.code == KeyCode.ENTER) {
                                doSearch()
                            }
                        }
                    }.textProperty().bindBidirectional(controller.userInput)
                    button("Search") {
                        style {
                            baseColor = Color.AQUA
                        }
                        action {
                            doSearch()
                        }
                    }

                    progressIndicator = progressindicator {
                        maxHeight = 20.0
                        maxWidth = 20.0
                        hide()
                    }
                }

                hbox {
                    paddingTop = 10
                    text("Results: ")
                    text("0").textProperty().bind(controller.resultsCount)
                }

            }

            /** TableView */
            hbox {
                paddingTop = 10
                fitToParentWidth()
                tableview(controller.results) {
                    readonlyColumn("Domain", Torrent::domain).maxWidth(100)
                    readonlyColumn("Added", Torrent::elapsedTimestamp)
                    readonlyColumn("Name", Torrent::filename).minWidth(400).maxWidth(800)
                    readonlyColumn("Seeders", Torrent::seeders).maxWidth(100)
                    readonlyColumn("Leechers", Torrent::leechers).maxWidth(100)
                    readonlyColumn("Comments", Torrent::commentsCount).maxWidth(100)
                    readonlyColumn("Downloads", Torrent::completions).maxWidth(100)

                    smartResize()
                    fitToParentWidth()

                    onDoubleClick {
                        openUrl(selectedItem)
                    }
                }
            }
        }

    override fun checksDone() {
        println()
    }

    private fun doSearch() {
        GlobalScope.launch {
            controller.userInput.value?.let {
                if (it.count() >= 3) {
                    progressBarState(true)

                    val requestJob = GlobalScope.launch {
                        controller.search()
                    }
                    requestJob.join()

                    progressBarState(false)
                }
            }
        }
    }

    private fun progressBarState(state: Boolean) {
        if (state) {
            progressIndicator.show()
        } else {
            progressIndicator.hide()
        }
    }

    private fun openUrl(item: Torrent?) {
        item?.let {
            hostServices.showDocument(it.url.unaccent())
            controller.itemDoubleClicked(it)
        }
    }
}