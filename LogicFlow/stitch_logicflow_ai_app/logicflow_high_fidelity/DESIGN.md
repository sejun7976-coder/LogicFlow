---
name: LogicFlow High-Fidelity
colors:
  surface: '#faf9fe'
  surface-dim: '#dad9df'
  surface-bright: '#faf9fe'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f4f3f8'
  surface-container: '#eeedf3'
  surface-container-high: '#e9e7ed'
  surface-container-highest: '#e3e2e7'
  on-surface: '#1a1b1f'
  on-surface-variant: '#414755'
  inverse-surface: '#2f3034'
  inverse-on-surface: '#f1f0f5'
  outline: '#717786'
  outline-variant: '#c1c6d7'
  surface-tint: '#005bc1'
  primary: '#0058bc'
  on-primary: '#ffffff'
  primary-container: '#0070eb'
  on-primary-container: '#fefcff'
  inverse-primary: '#adc6ff'
  secondary: '#006e28'
  on-secondary: '#ffffff'
  secondary-container: '#6ffb85'
  on-secondary-container: '#00732a'
  tertiary: '#4c4aca'
  on-tertiary: '#ffffff'
  tertiary-container: '#6664e4'
  on-tertiary-container: '#fffbff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d8e2ff'
  primary-fixed-dim: '#adc6ff'
  on-primary-fixed: '#001a41'
  on-primary-fixed-variant: '#004493'
  secondary-fixed: '#72fe88'
  secondary-fixed-dim: '#53e16f'
  on-secondary-fixed: '#002107'
  on-secondary-fixed-variant: '#00531c'
  tertiary-fixed: '#e2dfff'
  tertiary-fixed-dim: '#c2c1ff'
  on-tertiary-fixed: '#0c006a'
  on-tertiary-fixed-variant: '#3631b4'
  background: '#faf9fe'
  on-background: '#1a1b1f'
  surface-variant: '#e3e2e7'
typography:
  display:
    fontFamily: Inter
    fontSize: 34px
    fontWeight: '700'
    lineHeight: 41px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Inter
    fontSize: 22px
    fontWeight: '600'
    lineHeight: 28px
    letterSpacing: 0em
  body-lg:
    fontFamily: Inter
    fontSize: 17px
    fontWeight: '400'
    lineHeight: 22px
    letterSpacing: -0.01em
  body-md:
    fontFamily: Inter
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 20px
    letterSpacing: 0em
  label-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '500'
    lineHeight: 18px
    letterSpacing: 0.01em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 30px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  gutter: 20px
  margin-mobile: 16px
  margin-desktop: 40px
---

## Brand & Style

The visual identity of this design system is rooted in clarity, precision, and a premium editorial feel. It evokes an emotional response of reliability and "calm technology," where the interface recedes to let user content and logic take center stage. 

The design style is a sophisticated blend of **Modern Corporate** and **Glassmorphism**. It utilizes a "layered light" approach, where depth is communicated through soft, expansive shadows rather than rigid strokes. The aesthetic is intentionally airy, leveraging generous whitespace to reduce cognitive load and create a sense of systematic order.

## Colors

The palette is anchored by a pure white base to maximize contrast and a soft light gray for structural grouping. 

- **Primary:** Apple Blue (#007AFF) serves as the singular interaction color for actions, links, and active states.
- **Success:** A subtle Emerald (#34C759) is reserved strictly for positive confirmations and "complete" states.
- **Neutral Hierarchy:** Grayscale tones follow a strict functional path:
    - **Primary Text:** #1C1C1E (Off-black for better readability than pure black).
    - **Secondary Text:** #8E8E93 (For labels and hints).
    - **System Gray:** #F2F2F7 (Used for secondary backgrounds or "grouped" list sections).
    
In Dark Mode, the #FFFFFF background transitions to #000000, and the #F2F2F7 grouped background transitions to a deep #1C1C1E.

## Typography

This design system uses **Inter** as the primary typeface to emulate the San Francisco neo-grotesque style. The typographic scale is designed for high legibility with a focus on vertical rhythm.

Headlines should be bold and impactful, using negative letter-spacing at larger sizes to maintain a "tight" professional look. Body text defaults to a comfortable 17pt (Inter 17px) which mirrors Apple’s human interface standards for readability. Use "label-sm" for all-caps metadata or small captions to provide contrast in information density.

## Layout & Spacing

The layout utilizes a **fluid grid** system with strict horizontal margins. 

- **Desktop:** 12-column grid with 20px gutters and 40px outer margins.
- **Mobile:** 4-column grid with 16px gutters and 16px outer margins.

Spacing follows a 4px base unit. Component internal padding should default to 16px (md) for standard cards and 12px for smaller interactive elements. Generous whitespace between logical sections (32px or 48px) is required to maintain the "clean" aesthetic and prevent visual clutter.

## Elevation & Depth

This design system avoids heavy borders. Instead, depth is communicated through three distinct levels:

1.  **Level 0 (Base):** #FFFFFF background.
2.  **Level 1 (Card/Surface):** A subtle, diffused shadow. Use `box-shadow: 0 4px 12px rgba(0,0,0,0.05)`. This level is used for content containers and interactive buttons.
3.  **Level 2 (Overlays/Modals):** A deeper shadow for focused attention. Use `box-shadow: 0 8px 24px rgba(0,0,0,0.12)`.

**Glassmorphism:** Navigation bars and bottom toolbars must use a backdrop blur effect (`backdrop-filter: blur(20px)`) with a semi-transparent white background (`rgba(255, 255, 255, 0.8)`). This allows the content to peak through as the user scrolls, maintaining spatial awareness.

## Shapes

The shape language is defined by "squircle-adjacent" rounded corners. 

- **Standard Components:** 10px - 12px corner radius.
- **Large Containers/Cards:** 16px - 20px corner radius.
- **Inputs & Buttons:** Fixed 10px radius to ensure a consistent, friendly but professional appearance.

Rounded corners must be applied to all elements, including images and video containers, to maintain a unified visual language across the design system.

## Components

- **Buttons:** Primary buttons use a solid #007AFF fill with white text. Secondary buttons use #F2F2F7 background with #007AFF text. No borders.
- **Cards:** White background with a Level 1 shadow. Cards should have no border, using the shadow to define their silhouette against the #F2F2F7 background.
- **Input Fields:** A light gray background (#F2F2F7 or #E5E5EA) with a 10px radius. On focus, use a 2px #007AFF outer glow or soft border.
- **Glass Bars:** Top headers should have a 1px bottom stroke of #E5E5EA (or #38383A in dark mode) to define the edge of the glass effect.
- **Chips/Badges:** Pill-shaped (fully rounded) with low-saturation backgrounds (e.g., 10% opacity of the primary color) and high-saturation text for readability.
- **Lists:** Use the "Inset Grouped" style found in iOS—cards that span the width of their container with horizontal separators between items that don't touch the edges.