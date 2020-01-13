package boiler.timer

import react.RBuilder
import react.RComponent
import react.RProps
import react.RState

interface TimerProps : RProps {
    var count: Long?
//    var timeStep: Parameter
}

interface TimerState : RState {
//    var count: Long
}

class Timer(props: TimerProps) : RComponent<TimerProps, TimerState>(props) {
    /*override fun TimerState.init(props: TimerProps) {
        count = 0
    }*/

    /*private fun TimerState.update() {
        count += props.timeStep.normalized().toLong()
    }*/

//    private var timerID: Int? = null

    /*override fun componentDidMount() {
        timerID = window.setInterval({ setState { update() } }, 1000)
    }*/

    /*override fun componentWillUnmount() {
        window.clearInterval(timerID!!)
    }*/

    override fun RBuilder.render() {
        +"The boiler has been running for ${props.count ?: 0} seconds."
    }
}

fun RBuilder.ticker(handler: TimerProps.() -> Unit) = child(Timer::class) { attrs(handler) }
