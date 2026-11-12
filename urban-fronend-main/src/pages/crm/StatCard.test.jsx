import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { FiUsers } from "react-icons/fi";
import { StatCard } from "./StatCard";

describe("StatCard", () => {
  it("renders the title, value and icon", () => {
    const { container } = render(<StatCard title="Total leads" value={42} icon={FiUsers} tone="green" />);

    expect(screen.getByText("Total leads")).toBeInTheDocument();
    expect(screen.getByText("42")).toBeInTheDocument();
    expect(container.querySelector("svg")).not.toBeNull();
    expect(container.querySelector(".bg-emerald-50")).not.toBeNull();
  });

  it("tolerates a missing icon", () => {
    const { container } = render(<StatCard title="Empty" value={0} />);

    expect(screen.getByText("Empty")).toBeInTheDocument();
    expect(screen.getByText("0")).toBeInTheDocument();
    expect(container.querySelector("svg")).toBeNull();
  });
});
