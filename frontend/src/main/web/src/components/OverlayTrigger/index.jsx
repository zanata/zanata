import React, { cloneElement, Component, PropTypes } from 'react'
import ReactDOM from 'react-dom'
import contains from 'dom-helpers/query/contains'
import warning from 'warning'
import { pick } from 'lodash'
import createChainedFunction from '../../utils/createChainedFunction'
import Overlay from '../Overlay'
/**
 * Check if value one is inside or equal to the of value
 *
 * @param {string} one
 * @param {string|array} of
 * @returns {boolean}
 */
const isOrContains = (of, one) => {
  if (Array.isArray(of)) {
    return of.indexOf(one) >= 0
  }
  return one === of
}

class OverlayTrigger extends Component {
  state = {
    isOverlayShown: this.props.defaultOverlayShown
  }

  show = () => {
    this.setState({
      isOverlayShown: true
    })
  }

  hide = () => {
    this.setState({
      isOverlayShown: false
    })
  }

  toggle = () => {
    if (this.state.isOverlayShown) {
      this.hide()
    } else {
      this.show()
    }
  }

  /**
   * This is to preserve React context in "overlay" components
   * without resetting up all context.
   */
  renderOverlay = () => {
    ReactDOM.unstable_renderSubtreeIntoContainer(
      this, this._overlay, this._mountNode
    )
  }

  getOverlayTarget = () => {
    return ReactDOM.findDOMNode(this)
  }

  getOverlay = () => {
    const overlayProps = {
      ...pick(this.props, Object.keys(Overlay.propTypes)),
      show: this.state.isOverlayShown,
      placement: this.props.overlay.props.placement || this.props.placement,
      onHide: this.hide,
      target: this.getOverlayTarget,
      onExit: this.props.onExit,
      onExiting: this.props.onExiting,
      onExited: this.props.onExited,
      onEnter: this.props.onEnter,
      onEntering: this.props.onEntering,
      onEntered: this.props.onEntered
    }

    const overlay = cloneElement(this.props.overlay, {
      placement: overlayProps.placement,
      container: overlayProps.container
    })

    return (
      <Overlay {...overlayProps}>
        {overlay}
      </Overlay>
    )
  }
  handleDelayedShow = () => {
    if (this._hoverHideDelay !== undefined) {
      clearTimeout(this._hoverHideDelay)
      this._hoverHideDelay = undefined
      return
    }

    if (this.state.isOverlayShown || this._hoverShowDelay !== undefined) {
      return
    }

    const delay = this.props.delayShow != null
      ? this.props.delayShow : this.props.delay

    if (!delay) {
      this.show()
      return
    }

    this._hoverShowDelay = setTimeout(() => {
      this._hoverShowDelay = undefined
      this.show()
    }, delay)
  }

  handleDelayedHide = () => {
    if (this._hoverShowDelay !== undefined) {
      clearTimeout(this._hoverShowDelay)
      this._hoverShowDelay = undefined
      return
    }

    if (!this.state.isOverlayShown || this._hoverHideDelay !== undefined) {
      return
    }

    const delay = this.props.delayHide === undefined
      ? this.props.delay : this.props.delayHide

    if (!delay) {
      this.hide()
      return
    }

    this._hoverHideDelay = setTimeout(() => {
      this._hoverHideDelay = undefined
      this.hide()
    }, delay)
  }
  // Simple implementation of mouseEnter and mouseLeave.
  // React's built version is broken:
  // https://github.com/facebook/react/issues/4251
  // for cases when the trigger is disabled and mouseOut/Over can
  // cause flicker moving from one child element to another.
  handleMouseOverOut = (handler, e) => {
    let target = e.currentTarget
    let related = e.relatedTarget || e.nativeEvent.toElement

    if (!related || related !== target && !contains(target, related)) {
      handler(e)
    }
  }

  componentWillMount () {
    this.handleMouseOver =
      this.handleMouseOverOut.bind(null, this.handleDelayedShow)
    this.handleMouseOut =
      this.handleMouseOverOut.bind(null, this.handleDelayedHide)
  }

  componentDidMount () {
    this._mountNode = document.createElement('div')
    this.renderOverlay()
  }

   componentWillUnmount () {
    ReactDOM.unmountComponentAtNode(this._mountNode)
    this._mountNode = null
    clearTimeout(this._hoverShowDelay)
    clearTimeout(this._hoverHideDelay)
  }

  componentDidUpdate () {
    if (this._mountNode) {
      this.renderOverlay()
    }
  }

  render () {
    const trigger = React.Children.only(this.props.children)
    const triggerProps = trigger.props

    let props = {
      'aria-describedby': this.props.overlay.props.id
    }

    // create in render otherwise owner is lost...
    this._overlay = this.getOverlay()

    props.onClick = createChainedFunction(
      triggerProps.onClick,
      this.props.onClick
    )

    if (isOrContains(this.props.trigger, 'click')) {
      props.onClick = createChainedFunction(this.toggle, props.onClick)
    }

    if (isOrContains( this.props.trigger, 'hover')) {
      warning(!(this.props.trigger === 'hover'),
        `[zanata] Specifying only the "hover" trigger limits the
        visibilty of the overlay to just mouse users. Consider also including
        the "focus" trigger so that touch and keyboard only users can see
        the overlay as well.`)

      props.onMouseOver = createChainedFunction(
        this.handleMouseOver,
        this.props.onMouseOver,
        triggerProps.onMouseOver
      )
      props.onMouseOut = createChainedFunction(
        this.handleMouseOut,
        this.props.onMouseOut,
        triggerProps.onMouseOut
      )
    }

    if (isOrContains(this.props.trigger, 'focus')) {
      props.onFocus = createChainedFunction(
        this.handleDelayedShow,
        this.props.onFocus,
        triggerProps.onFocus
      )
      props.onBlur = createChainedFunction(
        this.handleDelayedHide,
        this.props.onBlur,
        triggerProps.onBlur
      )
    }

    return cloneElement(
      trigger,
      props
    )
  }

}

OverlayTrigger.propTypes = {
  ...Overlay.propTypes,
   /**
   * Specify which action or actions trigger Overlay visibility
   */
  trigger: PropTypes.arrayOf(PropTypes.oneOf(['click', 'hover', 'focus'])),
  /**
   * A millisecond delay amount to show and hide the Overlay once triggered
   */
  delay: PropTypes.number,
  /**
   * A millisecond delay amount before showing the Overlay once triggered.
   */
  delayShow: PropTypes.number,
  /**
   * A millisecond delay amount before hiding the Overlay once triggered.
   */
  delayHide: PropTypes.number,

  /**
   * The initial visibility state of the Overlay,
   * for more nuanced visibility control consider
   * using the Overlay component directly.
   */
  defaultOverlayShown: PropTypes.bool,

  /**
   * An element or text to overlay next to the target.
   */
  overlay: PropTypes.node.isRequired,

  /**
   * @private
   */
  onBlur: PropTypes.func,
  /**
   * @private
   */
  onClick: PropTypes.func,
  /**
   * @private
   */
  onFocus: PropTypes.func,
  /**
   * @private
   */
  onMouseEnter: PropTypes.func,
  /**
   * @private
   */
  onMouseLeave: PropTypes.func,
  // override specific overlay props
  /**
   * @private
   */
  target () {},
   /**
   * @private
   */
  onHide () {},
  /**
   * @private
   */
  show () {}
}

OverlayTrigger.defaultProps = {
  defaultOverlayShown: false,
  trigger: ['hover', 'focus'],
  delay: 300
}

export default OverlayTrigger
