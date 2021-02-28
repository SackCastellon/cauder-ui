package es.upv.mist.cauder.model

import com.ericsson.otp.erlang.OtpErlangTuple
import javafx.beans.property.ListProperty
import javafx.beans.property.SimpleListProperty
import javafx.collections.ObservableList
import tornadofx.*

class CauderSystem(
    mail: List<CauderMessage>,
    procs: List<CauderProcess>,
    val erlangRecord: OtpErlangTuple,
) {
    val mailProp: ListProperty<CauderMessage> = SimpleListProperty(mail.asObservable())
    var mail: ObservableList<CauderMessage> by mailProp

    val procsProp: ListProperty<CauderProcess> = SimpleListProperty(procs.asObservable())
    var procs: ObservableList<CauderProcess> by procsProp
}
