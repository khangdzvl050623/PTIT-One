<script setup lang="ts">
import { ref } from 'vue'
import heroImg from '../assets/hero.png'
import viteLogo from '../assets/vite.svg'
import vueLogo from '../assets/vue.svg'

const count = ref(0)
</script>

<template>
  <section :class="$style.center">
    <div :class="$style.hero">
      <img :src="heroImg" :class="$style.base" width="170" height="179" alt="" />
      <img :src="vueLogo" :class="$style.framework" alt="Vue logo" />
      <img :src="viteLogo" :class="$style.vite" alt="Vite logo" />
    </div>
    <div>
      <h1>Get started</h1>
      <p>Edit <code>src/App.vue</code> and save to test <code>HMR</code></p>
    </div>
    <button type="button" :class="$style.counter" @click="count++">
      Count is {{ count }}
    </button>
  </section>

  <div :class="$style.ticks"></div>

  <section :class="$style.nextSteps">
    <div :class="$style.docs">
      <svg :class="$style.icon" role="presentation" aria-hidden="true">
        <use href="/icons.svg#documentation-icon"></use>
      </svg>
      <h2>Documentation</h2>
      <p>Your questions, answered</p>
      <ul>
        <li>
          <a href="https://vite.dev/" target="_blank">
            <img :class="$style.logo" :src="viteLogo" alt="" />
            Explore Vite
          </a>
        </li>
        <li>
          <a href="https://vuejs.org/" target="_blank">
            <img :class="$style.buttonIcon" :src="vueLogo" alt="" />
            Learn more
          </a>
        </li>
      </ul>
    </div>
    <div :class="$style.social">
      <svg :class="$style.icon" role="presentation" aria-hidden="true">
        <use href="/icons.svg#social-icon"></use>
      </svg>
      <h2>Connect with us</h2>
      <p>Join the Vite community</p>
      <ul>
        <li>
          <a href="https://github.com/vitejs/vite" target="_blank">
            <svg :class="$style.buttonIcon" role="presentation" aria-hidden="true">
              <use href="/icons.svg#github-icon"></use>
            </svg>
            GitHub
          </a>
        </li>
        <li>
          <a href="https://chat.vite.dev/" target="_blank">
            <svg :class="$style.buttonIcon" role="presentation" aria-hidden="true">
              <use href="/icons.svg#discord-icon"></use>
            </svg>
            Discord
          </a>
        </li>
        <li>
          <a href="https://x.com/vite_js" target="_blank">
            <svg :class="$style.buttonIcon" role="presentation" aria-hidden="true">
              <use href="/icons.svg#x-icon"></use>
            </svg>
            X.com
          </a>
        </li>
        <li>
          <a href="https://bsky.app/profile/vite.dev" target="_blank">
            <svg :class="$style.buttonIcon" role="presentation" aria-hidden="true">
              <use href="/icons.svg#bluesky-icon"></use>
            </svg>
            Bluesky
          </a>
        </li>
      </ul>
    </div>
  </section>

  <div :class="$style.ticks"></div>
  <section :class="$style.spacer"></section>
</template>

<style module lang="scss">
@use '../styles/tokens' as tokens;

.hero {
  position: relative;

  .base,
  .framework,
  .vite {
    inset-inline: 0;
    margin: 0 auto;
  }

  .base {
    width: 170px;
    position: relative;
    z-index: 0;
  }

  .framework,
  .vite {
    position: absolute;
  }

  .framework {
    z-index: 1;
    top: 34px;
    height: 28px;
    transform: perspective(2000px) rotateZ(300deg) rotateX(44deg) rotateY(39deg)
      scale(1.4);
  }

  .vite {
    z-index: 0;
    top: 107px;
    height: 26px;
    width: auto;
    transform: perspective(2000px) rotateZ(300deg) rotateX(40deg) rotateY(39deg)
      scale(0.8);
  }
}

.counter {
  font-family: var(--mono);
  display: inline-flex;
  font-size: 16px;
  padding: 5px 10px;
  border-radius: 5px;
  color: var(--accent);
  background: var(--accent-bg);
  border: 2px solid transparent;
  transition: border-color 0.3s;
  margin-bottom: 24px;

  &:hover {
    border-color: var(--accent-border);
  }
  &:focus-visible {
    outline: 2px solid var(--accent);
    outline-offset: 2px;
  }
}

.center {
  display: flex;
  flex-direction: column;
  gap: 25px;
  place-content: center;
  place-items: center;
  flex-grow: 1;

  @media (max-width: tokens.$breakpoint-tablet) {
    padding: 32px 20px 24px;
    gap: 18px;
  }
}

.nextSteps {
  display: flex;
  border-top: 1px solid var(--border);
  text-align: left;

  & > div {
    flex: 1 1 0;
    padding: 32px;
    @media (max-width: tokens.$breakpoint-tablet) {
      padding: 24px 20px;
    }
  }

  .icon {
    margin-bottom: 16px;
    width: 22px;
    height: 22px;
  }

  @media (max-width: tokens.$breakpoint-tablet) {
    flex-direction: column;
    text-align: center;
  }

  ul {
    list-style: none;
    padding: 0;
    display: flex;
    gap: 8px;
    margin: 32px 0 0;

    .logo {
      height: 18px;
    }

    a {
      color: var(--text-h);
      font-size: 16px;
      border-radius: 6px;
      background: var(--social-bg);
      display: flex;
      padding: 6px 12px;
      align-items: center;
      gap: 8px;
      text-decoration: none;
      transition: box-shadow 0.3s;

      &:hover {
        box-shadow: var(--shadow);
      }

      .buttonIcon {
        height: 18px;
        width: 18px;
      }
    }

    @media (max-width: tokens.$breakpoint-tablet) {
      margin-top: 20px;
      flex-wrap: wrap;
      justify-content: center;

      li {
        flex: 1 1 calc(50% - 8px);
      }

      a {
        width: 100%;
        justify-content: center;
        box-sizing: border-box;
      }
    }
  }
}

.docs {
  border-right: 1px solid var(--border);

  @media (max-width: tokens.$breakpoint-tablet) {
    border-right: none;
    border-bottom: 1px solid var(--border);
  }
}

.social {
  @media (prefers-color-scheme: dark) {
    .buttonIcon {
      filter: invert(1) brightness(2);
    }
  }
}

.spacer {
  height: 88px;
  border-top: 1px solid var(--border);
  @media (max-width: tokens.$breakpoint-tablet) {
    height: 48px;
  }
}

.ticks {
  position: relative;
  width: 100%;

  &::before,
  &::after {
    content: '';
    position: absolute;
    top: -4.5px;
    border: 5px solid transparent;
  }

  &::before {
    left: 0;
    border-left-color: var(--border);
  }
  &::after {
    right: 0;
    border-right-color: var(--border);
  }
}
</style>
