import React, { PropTypes } from 'react'
import { Link, NavIcon } from './'
import { merge } from 'lodash'

/**
 * Item of side menu. See Nav.jsx for usage.
 */
const NavItem = ({
  id,
  link,
  small,
  icon,
  active,
  title,
  useHref,
  ...props
}) => {
  const isLogo = (icon === 'zanata')
  const isSearchLink = (link === '/search')
  const classes = {
    base: {
      bgc: '',
      c: 'C(light)!',
      d: 'D(n) D(f)--sm',
      fld: 'Fld(c)',
      ai: 'Ai(c)',
      flxg: 'Flxg(1) Flxg(0)--sm',
      flxs: 'Flxs(0)',
      fz: 'Fz(msn2)',
      p: 'P(rq) Px(rq)--sm Py(rh)--sm',
      ta: 'Ta(c)',
      trs: 'Trs(aeo)',
      hover: {
        c: 'C(white)!:h',
        bgc: 'Bgc(#fff.2):h',
        filter: ''
      },
      focus: {
        filter: ''
      },
      active: {
        filter: ''
      }
    },
    active: {
      bgc: 'Bgc(white)',
      bxsh: 'Bxsh(sh1)',
      c: 'C(pri)!',
      cur: 'Cur(d)',
      hover: {
        c: '',
        bgc: ''
      }
    },
    small: {
      d: 'D(f)'
    },
    search: {
      m: 'My(rh)'
    }
  }
  const themeClasses = {
    base: merge({},
      classes.base,
      active && !isLogo ? classes.active : {},
      small ? classes.small : {},
      isSearchLink ? classes.search : {}
    )
  }
  const text = isLogo ? (<span className='Hidden'>{title}</span>) : title
  return (
    <Link {...props} id={id} link={link} theme={themeClasses} useHref={useHref}>
      <NavIcon name={icon} size={isLogo ? '6' : '1'} />
      {text}
    </Link>
  )
}

NavItem.propTypes = {
  id: PropTypes.string,
  link: PropTypes.string,
  small: PropTypes.bool,
  icon: PropTypes.string,
  active: PropTypes.bool,
  title: PropTypes.string,
  useHref: PropTypes.bool
}

export default NavItem
